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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.MapMarshaller;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.publish.STSInstanceConfigPersister;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.security.AccessController;
import java.util.List;
import java.util.Map;

public class RestSTSSMSInstanceConfigPersister implements STSInstanceConfigPersister<RestSTSInstanceConfig> {
    private static final String SERVICE_NAME = "RestSecurityTokenService";//taken from restSTS.xml - define in AMSTSConstants. TODO
    private static final int PRIORITY_ZERO = 0;

    private final SSOToken adminToken;
    private final MapMarshaller<RestSTSInstanceConfig> instanceConfigMapMarshaller;
    private final Logger logger;

    @Inject
    RestSTSSMSInstanceConfigPersister(MapMarshaller<RestSTSInstanceConfig> instanceConfigMapMarshaller, Logger logger) {
        this.instanceConfigMapMarshaller = instanceConfigMapMarshaller;
        adminToken =  AccessController.doPrivileged(AdminTokenAction.getInstance());
        this.logger = logger;
    }

    public synchronized void persistSTSInstance(String stsInstanceId, RestSTSInstanceConfig instance) throws STSInitializationException {
        try {
            /*
            Model for code below taken from AMAuthenticationManager.createAuthenticationInstance, as the 'multiple authN module per realm'
            model applies to the STS, and the AMAuthenticationManager seems to implement the SMS persistence concern of these semantics.
             */
            OrganizationConfigManager organizationConfigManager =
                    new OrganizationConfigManager(adminToken, instance.getDeploymentConfig().getRealm());  //TODO: get from factory to avoid new
            Map<String, Object> instanceConfigAttributes = instanceConfigMapMarshaller.marshallAttributesToMap(instance);

            if (!organizationConfigManager.getAssignedServices().contains(SERVICE_NAME)) {
                organizationConfigManager.assignService(SERVICE_NAME, null);
            }
            ServiceConfig orgConfig = organizationConfigManager.getServiceConfig(SERVICE_NAME);
            if (orgConfig == null) {
                orgConfig = organizationConfigManager.addServiceConfig(SERVICE_NAME, null);
            }
            orgConfig.addSubConfig(stsInstanceId, ISAuthConstants.SERVER_SUBSCHEMA,
                        PRIORITY_ZERO, instanceConfigAttributes);
            if (logger.isDebugEnabled()) {
                logger.debug("Persisted sts instance with id " + stsInstanceId + " in realm " + instance.getDeploymentConfig().getRealm());
            }

        } catch (SMSException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught persisting RestSTSInstanceConfig instance: " + e, e);
        } catch (SSOException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught persisting RestSTSInstanceConfig instance: " + e, e);
        }
    }

    public void removeSTSInstance(String key, String realm) {

    }

    public RestSTSInstanceConfig getSTSInstanceConfig(String stsInstanceId, String realm) throws STSInitializationException {
        try {
            /*
            Model for code below taken from AMAuthenticationManager.getAuthenticationInstance, as the 'multiple authN module per realm'
            model applies to the STS, and the AMAuthenticationManager seems to implement the SMS persistence concern of these semantics.
             */
            ServiceConfig baseService = new ServiceConfigManager(SERVICE_NAME, adminToken).getOrganizationConfig(realm, null);
            if (baseService != null) {
                ServiceConfig instanceService = baseService.getSubConfig(stsInstanceId);
                if (instanceService != null) {
                    Map<String, Object> instanceAttrs = instanceService.getAttributes();
                    return RestSTSInstanceConfig.marshalFromAttributeMap(instanceAttrs);
                } else {
                    throw new STSInitializationException(ResourceException.INTERNAL_ERROR,
                            "Error reading RestSTSInstanceConfig instance from SMS: no instance state in realm " + realm
                                    + " corresponding to instance id " + stsInstanceId);
                }
            } else {
                throw new STSInitializationException(ResourceException.INTERNAL_ERROR,
                        "Error reading RestSTSInstanceConfig instance from SMS: no base instance state in realm " + realm);
            }
        } catch (SSOException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught reading RestSTSInstanceConfig instance from SMS: " + e, e);
        } catch (SMSException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught reading RestSTSInstanceConfig instance from SMS: " + e, e);
        } catch (IllegalStateException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught reading RestSTSInstanceConfig instance from SMS: " + e, e);
        }
    }

    public List<RestSTSInstanceConfig> getAllPublishedInstances() {
        return null;
    }
}
