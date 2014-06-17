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
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.RestSTSInstanceModule;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.publish.RestSTSInstancePublisher;
import org.slf4j.Logger;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

public class RestSTSPublishService implements SingletonResourceProvider {
    private static final String ADD_INSTANCE = "add_instance";
    private static final String REMOVE_INSTANCE = "remove_instance";
    private static final String REALM = "realm";
    private static final String STS_ID = "sts_id";
    private static final String RESULT = "result";
    private static final String SUCCESS = "success";
    private static final String URL_ELEMENT = "url_element";
    private final RestSTSInstancePublisher publisher;
    private final Logger logger;

    public RestSTSPublishService(RestSTSInstancePublisher publisher, Logger logger) {
        this.publisher = publisher;
        this.logger = logger;
    }

    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        HttpContext httpContext = context.asContext(HttpContext.class);
        final String action = request.getAction();
        if (ADD_INSTANCE.equals(action)) {
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
                urlElement = instanceConfig.getDeploymentSubPath();
                publisher.publishInstance(instanceConfig, instanceInjector.getInstance(RestSTS.class), urlElement);
                if (logger.isDebugEnabled()) {
                    logger.debug("rest sts instance successfully published at " + urlElement);
                }
                handler.handleResult(json(object(field(RESULT, SUCCESS), field(URL_ELEMENT, urlElement))));
            } catch (STSPublishException e) {
                String message = "Exception caught publishing instance: at url " + urlElement + ". Exception" + e;
                logger.error(message, e);
                handler.handleError(e);
            } catch (Exception e) {
                String message = "Exception caught publishing instance: at url " + urlElement + ". Exception" + e;
                logger.error(message, e);
                handler.handleError(new InternalServerErrorException(message, e));
            }
        } else if (REMOVE_INSTANCE.equals(action)) {
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

    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        /*
        This should be updated to call publisher.getPublishedInstances().
         */
        handler.handleError(new NotSupportedException());
    }

    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }
}
