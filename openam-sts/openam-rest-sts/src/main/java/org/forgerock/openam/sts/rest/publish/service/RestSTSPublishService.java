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

package org.forgerock.openam.sts.rest.publish.service;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.RestSTSInstanceModule;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.publish.RestSTSInstancePublisher;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.slf4j.Logger;

import java.util.List;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/*
Because this class will allow callers to create/delete/read Rest STS instances, it is tempting to consider it a
CollectionResourceProvider. However, re-implementing this service as a CollectionResourceProvider becomes problematic
because each Rest STS instance is represented by its unique identifier, which is the url-element at which it is exposed
relative to the rest-sts-servlet-base, a url-element which can include realm elements - e.g. realm_a/realm_b/sts_instance_123.
It is tempting to think that when a CollectionResourceProvider creates a Rest STS instance with a resource_id of
realm_a/realm_b/sts_instance_123, you should be able to issue a delete with this same url-element, and have the Rest STS instance
disappear. However, when you add a route with a CollectionResourceProvider, it will give you a 404 if you e.g. issue a delete
at publish/realm_a/realm_b/sts_instance_123. This is because adding a route with a CollectionResourceProvider adds a sub-router
with two paths - one for {id} and one for "", and these routes won't match a path that includes '/'.
See Router#addRoute(uriTemplate, CollectionResourceProvider) and Resources#newCollection for details.
This problem can be avoided by following the idiom defined in the DynamicRealmDemo
class in json-resource-examples, but this idiom does not really fit either, as it is used to expose the same service in
different realms. This pattern does not really fit with the Rest STS publish service, as it does not provide realm-specific
information, but rather is a global service which is used to publish Rest STS instances into a specific realm. Thus it should
not be exposed in multiple realms, but rather be invoked with a resource id which can include a realm path. This idiom best
fits for the SingletonResourceProvider.
 */
public class RestSTSPublishService implements SingletonResourceProvider {
    private static final String REALM = "realm";
    private static final String STS_ID = "sts_id";
    private static final String RESULT = "result";
    private static final String SUCCESS = "success";
    private static final String PUBLISHED_INSTANCES = "published_instances";
    private final RestSTSInstancePublisher publisher;
    private final Logger logger;

    public RestSTSPublishService(RestSTSInstancePublisher publisher, Logger logger) {
        this.publisher = publisher;
        this.logger = logger;
    }

    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        HttpContext httpContext = context.asContext(HttpContext.class);
        final String action = request.getAction();
        if (AMSTSConstants.REST_STS_PUBLISH_SERVICE_ACTION_ADD_INSTANCE.equals(action) ||
                AMSTSConstants.REST_STS_PUBLISH_SERVICE_ACTION_REPUBLISH_INSTANCE.equals(action)) {
            RestSTSInstanceConfig instanceConfig;
            try {
                instanceConfig = RestSTSInstanceConfig.fromJson(request.getContent());
            } catch (Exception e) {
                logger.error("Exception caught marshalling json into RestSTSInstanceConfig instance: " + e);
                handler.handleError(new BadRequestException(e));
                return;
            }
            Injector instanceInjector;
            try {
                instanceInjector = Guice.createInjector(new RestSTSInstanceModule(instanceConfig));
            } catch (Exception e) {
                String message = "Exception caught creating the guice injector corresponding to rest sts instance: " + e;
                logger.error(message);
                handler.handleError(new InternalServerErrorException(message, e));
                return;
            }
            String urlElement = null;
            try {
                boolean republish = AMSTSConstants.REST_STS_PUBLISH_SERVICE_ACTION_REPUBLISH_INSTANCE.equals(action);
                urlElement =
                        publisher.publishInstance(instanceConfig, instanceInjector.getInstance(RestSTS.class), republish);
                if (logger.isDebugEnabled()) {
                    logger.debug("rest sts instance successfully published at " + urlElement);
                }
                handler.handleResult(json(object(field(RESULT, SUCCESS),
                        field(AMSTSConstants.SUCCESSFUL_REST_STS_PUBLISH_URL_ELEMENT, urlElement))));
            } catch (STSPublishException e) {
                String message = "Exception caught publishing instance: at url " + urlElement + ". Exception" + e;
                logger.error(message, e);
                handler.handleError(e);
            } catch (Exception e) {
                String message = "Exception caught publishing instance: at url " + urlElement + ". Exception" + e;
                logger.error(message, e);
                handler.handleError(new InternalServerErrorException(message, e));
            }
        } else if (AMSTSConstants.REST_STS_PUBLISH_SERVICE_ACTION_REMOVE_INSTANCE.equals(action)) {
            String realm = httpContext.getParameterAsString(REALM);
            if (realm == null) {
                handler.handleError(new BadRequestException("The " + REALM + " query parameter has not been specified."));
            }
            String stsId = httpContext.getParameterAsString(STS_ID);
            if (stsId == null) {
                handler.handleError(new BadRequestException("The " + STS_ID + " query parameter has not been specified."));
            }
            try {
                publisher.removeInstance(stsId, realm);
                if (logger.isDebugEnabled()) {
                    logger.debug("rest sts instance " + stsId + " successfully removed from realm " + realm);
                }
                handler.handleResult(json(object(field(RESULT, "rest sts instance " + stsId + " successfully removed from realm " + realm))));
            } catch (STSPublishException e) {
                String message = "Exception caught removing instance: " + stsId + " from realm " + realm + ". Exception:" + e;
                logger.error(message, e);
                handler.handleError(e);
            } catch (Exception e) {
                String message = "Exception caught removing instance: " + stsId + " from realm " + realm + ". Exception:" + e;
                logger.error(message, e);
                handler.handleError(new InternalServerErrorException(message, e));
            }
        } else {
            handler.handleError(new BadRequestException("The specified _action is not supported."));
        }
    }

    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /*
    This method is currently not consumed, as the RestSTSInstanceRepublishServlet consumes the RestSTSInstancePublisher
    directly. It is maintained to support the remote deployment of Rest STS instances, and as a means to remotely obtain
    the inventory of published Rest STS instances.
     */
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            List<RestSTSInstanceConfig> publishedInstances = publisher.getPublishedInstances();
            JsonObject jsonObject = JsonValueBuilder.jsonValue();
            for (RestSTSInstanceConfig instanceConfig : publishedInstances) {
                jsonObject.put(instanceConfig.getDeploymentSubPath(), instanceConfig.toJson().toString());
            }
            /*
            Note that the revision etag is not set, as this is not a resource which should really be cached.
            If caching becomes necessary, a string composed of the hash codes of each of the RestSTSInstanceConfig
            instances could be used (or a hash of that string).
             */
            handler.handleResult(new Resource(PUBLISHED_INSTANCES, "", jsonObject.build()));
        } catch (STSPublishException e) {
            String message = "Exception caught obtaining list of previously published rest sts instances: " + e;
            logger.error(message, e);
            handler.handleError(new InternalServerErrorException(message, e));
        }
    }

    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }
}
