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

package com.sun.identity.console.reststs.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.console.base.model.AMSystemConfig;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.fluent.JsonValue;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * @see com.sun.identity.console.reststs.model.RestSTSModel
 * This class extends the AMServiceProfileModelImpl because this class provides functionality for reading values corresponding
 * to propertySheets.
 *
 */
public class RestSTSModelImpl extends AMServiceProfileModelImpl implements RestSTSModel {
    /*
    Review TODO: perhaps the values below which match those defined in AMSTSConstants should be moved to a module
    shared between openam-sts and openam-console? Perhaps openam-shared?? Or should the values just be duplicated??
     */

    /*
    This field matches that defined in the AMSTSConstants. I cannot introduce a dependency on the rest-sts in the
    openam-console module, so this value must be duplicate here.
     */
    private static final String REST_STS_PUBLISH_INVOCATION_CONTEXT = "invocation_context";

    /*
    This field matches that defined in the AMSTSConstants. I cannot introduce a dependency on the rest-sts in the
    openam-console module, so this value must be duplicate here.
     */
    private static final String REST_STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN = "invocation_context_view_bean";

    /*
    This field matches that defined in the AMSTSConstants. I cannot introduce a dependency on the rest-sts in the
    openam-console module, so this value must be duplicate here.
     */
    private static final String REST_STS_PUBLISH_INSTANCE_STATE = "instance_state";

    /*
    This String must match the string defined in STSInstanceConfig.AM_DEPLOYMENT_URL.
     */
    private static final String AM_DEPLOYMENT_URL = "am-deployment-url";

    private static final String DEPLOYMENT_REALM = "deployment-realm";

    private static final String FORWARD_SLASH = "/";

    public RestSTSModelImpl(HttpServletRequest req, Map map) throws AMConsoleException {
        super(req, REST_STS_SERVICE_NAME, map);
    }

    public Set<String> getPublishedInstances(String realm) throws AMConsoleException {
        try {
            ServiceConfig baseService = new ServiceConfigManager(REST_STS_SERVICE_NAME,
                    getAdminToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                return baseService.getSubConfigNames();
            } else {
                return Collections.EMPTY_SET;
            }
        } catch (SMSException e) {
            throw new AMConsoleException(e);
        } catch (SSOException e) {
            throw new AMConsoleException(e);
        }
    }

    public void deleteInstances(String realm, Set<String> instanceNames) throws AMConsoleException {
        for (String instanceName : instanceNames) {
            try {
                RestSTSModelResponse response = deleteInstance(realm, instanceName);
                if (!response.isSuccessful()) {
                    throw new AMConsoleException(response.getMessage());
                }
            } catch (IOException e) {
                throw new AMConsoleException(e);
            }
        }
    }

    public RestSTSModelResponse createInstance(Map<String, Set<String>> configurationState, String realm) throws AMConsoleException {
        addProgrammaticConfigurationState(configurationState, realm);
        JsonValue propertiesMap = new JsonValue(marshalSetValuesToListValues(configurationState));
        JsonValue invocationJson = json(object(
                field(REST_STS_PUBLISH_INVOCATION_CONTEXT, REST_STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN),
                field(REST_STS_PUBLISH_INSTANCE_STATE, propertiesMap)));
        try {
            return invokeRestSTSPublishService(invocationJson.toString());
        } catch (IOException e) {
            throw new AMConsoleException(e);
        }
    }

    public RestSTSModelResponse updateInstance(Map<String, Set<String>> configurationState, String realm, String instanceName) throws AMConsoleException {
        return RestSTSModelResponse.success("Instance " + instanceName + " in realm " + realm  + " updated.");
        //TODO: proper implementation, when rest sts publish service updated.
    }

    public Map<String, Set<String>> getInstanceState(String realm, String instanceName) throws AMConsoleException {
        try {
            ServiceConfig baseService = new ServiceConfigManager(REST_STS_SERVICE_NAME,
                    getAdminToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                ServiceConfig serviceConfig = baseService.getSubConfig(instanceName);
                if (serviceConfig != null) {
                    return serviceConfig.getAttributes();
                } else {
                    return Collections.EMPTY_MAP;
                }
            } else {
                return Collections.EMPTY_MAP;
            }
        } catch (SMSException e) {
            throw new AMConsoleException(e);
        } catch (SSOException e) {
            throw new AMConsoleException(e);
        }
    }

    public RestSTSModelResponse validateConfigurationState(Map<String, Set<String>> configurationState) {
        //TODO - I18N of messages, and statics for model fields.
        if (isNullOrEmpty(configurationState.get("saml2-token-lifetime-seconds"))) {
            return RestSTSModelResponse.failure("Saml2 token lifetime must be specified");
        }

        if (isNullOrEmpty(configurationState.get("deployment-url-element"))) {
            return RestSTSModelResponse.failure("Deyployment Url element must be specified.");
        } else {
            String urlElement = configurationState.get("deployment-url-element").iterator().next();
            if (urlElement.contains("/")) {
                return RestSTSModelResponse.failure("Deployment Url element can neither start, end, nor contain, the '/' character.");
            }
        }

        if (isNullOrEmpty(configurationState.get("issuer-name"))) {
            return RestSTSModelResponse.failure("Issuer name must be specified.");
        }
        //keystore state - perhaps open it?? at least has to be specified
        return RestSTSModelResponse.success();
    }

    /*
    Add the url corresponding to the am deployment, and the realm, as this information does not have to be solicited from the user.
     */
    private void addProgrammaticConfigurationState(Map<String, Set<String>> configurationState, String realm) {
        Set<String> deploymentUrlSet = new HashSet<String>();
        deploymentUrlSet.add(getAMDeploymentUrl());
        configurationState.put(AM_DEPLOYMENT_URL, deploymentUrlSet);

        Set<String> deploymentRealmSet = new HashSet<String>();
        deploymentRealmSet.add(realm);
        configurationState.put(DEPLOYMENT_REALM, deploymentRealmSet);
    }

    private RestSTSModelResponse invokeRestSTSPublishService(String invocationPayload) throws IOException {
        URL url = new URL(getRestSTSInstanceCreationUrl());
        HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
            return RestSTSModelResponse.success(getSuccessMessage(connection));
        } else {
            return RestSTSModelResponse.failure(getErrorMessage(connection));
        }
    }

    private String getSuccessMessage(HttpURLConnection connection) throws IOException {
        return readInputStream(connection.getInputStream());
    }

    private String getErrorMessage(HttpURLConnection connection) throws IOException {
        //TODO: not sure I should get content of input or error stream
        if (connection.getErrorStream() != null) {
            return readInputStream(connection.getErrorStream());
        } else {
            return readInputStream(connection.getInputStream());
        }
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "Empty error stream";
        } else {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            try {
                String str;
                while ((str = bufferedReader.readLine()) != null) {
                    stringBuilder.append(str);
                }
                return stringBuilder.toString();
            } finally {
                try {
                    bufferedReader.close();
                } catch (IOException ioe) {
                    debug.warning("Exception closing BufferedReader in finally block in RestSecurityTokenViewBean#readInputStream: "
                            + ioe.getMessage(), ioe);
                }
            }
        }
    }
    private String getRestSTSInstanceDeletionUrl(String realm, String instanceId) {
        String processedInstanceId = instanceId;
        if (processedInstanceId.startsWith(FORWARD_SLASH)) {
            processedInstanceId = processedInstanceId.substring(1);
        }

        if ("/".equals(realm)) {
            return getRestSTSPublishEndpointUrl() + FORWARD_SLASH + processedInstanceId;
        } else {
            return getRestSTSPublishEndpointUrl() + FORWARD_SLASH + realm + FORWARD_SLASH + processedInstanceId;
        }
    }

    private String getRestSTSInstanceCreationUrl() {
        //TODO: define static for this - or a commons property class - available in openam-shared??
        return getRestSTSPublishEndpointUrl() + "?_action=create";
    }

    private String getRestSTSPublishEndpointUrl() {
        return getAMDeploymentUrl() + "/rest-sts-publish/publish";
    }

    private String getAMDeploymentUrl() {
        return AMSystemConfig.serverURL + AMSystemConfig.serverDeploymentURI;
    }

    /*
    Currently, JsonValue#toString will only create a json array for elements which are lists. If I want the
    Map<String, Set<String>> returned by this.getValues() to marshal to json correctly using JsonValue#toString(), I
    need to transform the Map<String, Set<String>> to a Map<String, List<String>>.
     */
    private Map<String, List<String>> marshalSetValuesToListValues(Map<String, Set<String>> smsMap) {
        Map<String, List<String>> listMap = new HashMap<String, List<String>>();
        for (Map.Entry<String, Set<String>> entry : smsMap.entrySet()) {
            ArrayList<String> list = new ArrayList<String>(entry.getValue().size());
            list.addAll(entry.getValue());
            listMap.put(entry.getKey(), list);
        }
        return listMap;
    }

    private boolean isNullOrEmpty(Set<String> set) {
        return ((set == null) || set.isEmpty());
    }

    private SSOToken getAdminToken()  {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    private RestSTSModelResponse deleteInstance(String realm, String instanceId) throws IOException {
        return invokeRestSTSInstanceDeletion(getRestSTSInstanceDeletionUrl(realm, instanceId));
    }

    private RestSTSModelResponse invokeRestSTSInstanceDeletion(String deletionUrl) throws IOException {
        URL url = new URL(deletionUrl);
        HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
        connection.setDoOutput(true);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.connect();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return RestSTSModelResponse.success(getSuccessMessage(connection));
        } else {
            return RestSTSModelResponse.failure(getErrorMessage(connection));
        }
    }
}
