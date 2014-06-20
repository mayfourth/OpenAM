/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.publish;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.rest.config.RestSTSInjectorHolder;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.restlet.data.MediaType;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 *  This class is an HttpServlet which implements only the init method. All other methods return 400/405 errors as
 *  implemented in the HttpServlet class. The init method will re-constitute previously-published Rest STS instances by
 *  1. calling a GET on the publish service to obtain the configuration corresponding to previously-published instances
 *  and 2. POSTing to the publish service with the configuration instances obtained from #1 to re-expose these
 *  previously-published instances.
 */
public class RestSTSInstanceRepublishServlet extends HttpServlet {
    private final Logger logger;

    public RestSTSInstanceRepublishServlet() {
        logger = RestSTSInjectorHolder.getInstance(Key.get(Logger.class));
    }

    @Override
    public void init() throws ServletException {
        /*
        We will enter this branch if the RestSTSModule missed some bindings, causing the injector creation in the
        RestSTSInjectorHolder to fail.
         */
        if (logger == null) {
            throw new ServletException("Could not obtain logger from RestSTSInjectorHolder in " +
                    "RestSTSInstanceReconstitutionServlet#init method. This means that the RestSTSModule is missing " +
                    "some bindings!");
        }
        String publishUrl = SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL) + "://" +
                SystemPropertiesManager.get(Constants.AM_SERVER_HOST) + ":" +
                SystemPropertiesManager.get(Constants.AM_SERVER_PORT) +
                SystemPropertiesManager.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR) +
                RestSTSInjectorHolder.getInstance(Key.get(String.class,
                        Names.named(AMSTSConstants.REST_STS_PUBLISH_SERVICE_URI_ELEMENT)));
        List<RestSTSInstanceConfig> publishedInstances;
        try {
            publishedInstances = getPublishedInstances(publishUrl);
        } catch (Exception e) {
            /*
            Catch block to catch the UriSyntaxException, IOException and the ResourceException thrown by Restlet, and the
            IllegalStateException thrown by RestSTSInstanceConfig#fromJson.
             */
            String message = "Exception caught obtaining previously published Rest STS instances from the publish " +
                    "service at url " + publishUrl + "; Exception: " + e;
            logger.error(message, e);
            throw new ServletException(message);
        }
        republishInstances(publishUrl, publishedInstances);
    }

    private List<RestSTSInstanceConfig> getPublishedInstances(String publishUrl) throws URISyntaxException, IOException {
        ClientResource resource = new ClientResource(new URI(publishUrl));
        Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(AMSTSConstants.RESTLET_HEADER_KEY);
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put(AMSTSConstants.RESTLET_HEADER_KEY, headers);
        }
        headers.set(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        headers.set(AMSTSConstants.ACCEPT, AMSTSConstants.APPLICATION_JSON);
        String response = resource.get().getText();
        List<RestSTSInstanceConfig> instanceConfigs = new ArrayList<RestSTSInstanceConfig>();
        JsonValue json = JsonValueBuilder.toJsonValue(response);
        for (String key : json.asMap().keySet()) {
            JsonValue value = json.get(key);
            try {
                RestSTSInstanceConfig instanceConfig = RestSTSInstanceConfig.fromJson(JsonValueBuilder.toJsonValue(value.asString()));
                instanceConfigs.add(instanceConfig);
            } catch (RuntimeException e) {
                logger.error("Exception marshalling RestSTSInstanceConfig instance " + key +
                        ". Instance will not be republished. Exception: " + e);
                continue;
            }
        }
        return instanceConfigs;
    }

    private void republishInstances(String publishUrlBase, List<RestSTSInstanceConfig> publishedInstances) {
        ClientResource clientResource = new ClientResource(createRePublishUrl(publishUrlBase));
        for (RestSTSInstanceConfig instanceConfig : publishedInstances) {
            Representation representation;
            try {
                representation = clientResource.post(new StringRepresentation(instanceConfig.toJson().toString()),
                        MediaType.APPLICATION_JSON);
            } catch (ResourceException e) {
                logger.error("Exception caught republishing Rest STS instance " + instanceConfig.getDeploymentSubPath()
                        + "; Exception: " + e, e);
                continue;
            }
            String response;
            try {
                response = representation.getText();
            } catch (IOException e) {
                logger.error("Exception caught republishing Rest STS instance " + instanceConfig.getDeploymentSubPath() +
                        "; Exception: " + e);
                continue;
            }
            JsonValue responseJson;
            try {
                responseJson = JsonValueBuilder.toJsonValue(response);
            } catch (JsonException e) {
                logger.error("Exception caught marshalling response to republishing Rest STS instance "
                        + instanceConfig.getDeploymentSubPath() + ". The response: " + response + ". The exception: " + e, e);
                continue;
            }
            JsonValue deploymentSubPath = responseJson.get(AMSTSConstants.SUCCESSFUL_REST_STS_PUBLISH_URL_ELEMENT);
            if ((deploymentSubPath == null) || !deploymentSubPath.isString()) {
                logger.warn("The response from publish service while republishing Rest STS instance with deployment url "
                        + instanceConfig.getDeploymentSubPath() + " unexpected. The response: " + responseJson.toString());
            } else {
                if (!deploymentSubPath.asString().equals(instanceConfig.getDeploymentSubPath())) {
                    logger.warn("Unexpected state re-constituting Rest STS instance. Response from publish service indicates url of " +
                            deploymentSubPath.asString() + " while the RestSTSInstanceConfig instance indicates a subpath of " +
                            instanceConfig.getDeploymentSubPath());
                } else {
                    logger.info("Successfully republished Rest STS instance at path " + instanceConfig.getDeploymentSubPath());
                }
            }
        }
    }

    private String createRePublishUrl(String publishUrlBase) {
        return publishUrlBase + "_action=" + AMSTSConstants.REST_STS_PUBLISH_SERVICE_ACTION_REPUBLISH_INSTANCE;
    }
}
