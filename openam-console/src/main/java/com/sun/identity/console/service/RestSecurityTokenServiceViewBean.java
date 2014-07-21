package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.PageTrail;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.base.model.AMSystemConfig;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.web.ui.view.alert.CCAlert;
import org.forgerock.json.fluent.JsonValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

public class RestSecurityTokenServiceViewBean extends AMServiceProfileViewBeanBase {
    private static final Debug debug = Debug.getInstance("sts");
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

    public static final String DEFAULT_DISPLAY_URL =
            "/console/service/RestSecurityTokenService.jsp";

    public static final String PAGE_MODIFIED = "pageModified";

    public RestSecurityTokenServiceViewBean() {
        super("RestSecurityTokenService",
                DEFAULT_DISPLAY_URL,
                "RestSecurityTokenService");
    }

    protected void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
            createPropertyModel();
            createPageTitleModel();
            registerChildren();
        }
    }

    protected void registerChildren() {
        super.registerChildren();
    }

    protected void createPageTitleModel() {
        createThreeButtonPageTitleModel();
    }

    protected void createPropertyModel() {
        String xmlFileName = "com/sun/identity/console/propertyRestSecurityTokenService.xml";
        String xml = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xmlFileName));

        propertySheetModel = new AMPropertySheetModel(xml);
        propertySheetModel.clear();
    }

    /**
     * Handles save button request.
     * save
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) throws ModelControlException {
        submitCycle = true;
        Map<String, Set<String>> configurationState = (Map<String, Set<String>>)getAttributeSettings();
        addOpenAMDeploymentUrlToConfigurationState(configurationState);
        if (configurationStateValid(configurationState)) {
            JsonValue propertiesMap = new JsonValue(marshalSetValuesToListValues(configurationState));
            JsonValue invocationJson = json(object(
                    field(REST_STS_PUBLISH_INVOCATION_CONTEXT, REST_STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN),
                    field(REST_STS_PUBLISH_INSTANCE_STATE, propertiesMap)));
            invokeRestSTSPublishService(invocationJson.toString());
        }
        forwardTo();
    }

    /*
    Returns a map of all settings, including those not changed from the default values in the model.
     */
    private Map getAttributeSettings() throws ModelControlException {
        Map values = null;
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();

        if (model != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            try {
                values = ps.getAttributeValues(model.getAttributeValues(), false, model);
            } catch (AMConsoleException e) {
                throw new ModelControlException(e.getMessage(), e);
            }
        }
        return values;
    }

    /*
    Add the url corresponding to the am deployment, as this information does not have to be solicited from the user.
     */
    private void addOpenAMDeploymentUrlToConfigurationState(Map<String, Set<String>> configurationState) {
        Set<String> deploymentUrlSet = new HashSet<String>();
        deploymentUrlSet.add(getAMDeploymentUrl());
        configurationState.put(AM_DEPLOYMENT_URL, deploymentUrlSet);
    }

    /*
    performs validation checks on user-configured state.
     */
    private boolean configurationStateValid(Map<String, Set<String>> configurationState) {
        if (isNullOrEmpty(configurationState.get("saml2-token-lifetime-seconds"))) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "SAML2 Token lifetime must be specified.");
            return false;
        }
        if (isNullOrEmpty(configurationState.get("deployment-realm"))) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "Deployment realm must be specified.");
            return false;
        } else {
            String realm = configurationState.get("deployment-realm").iterator().next();
            if (!isValidRealm(realm)) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        "Specified deployment realm is not a valid realm.");
                return false;
            }
        }
        if (isNullOrEmpty(configurationState.get("deployment-url-element"))) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "Deyployment Url element must be specified.");
            return false;
        } else {
            String urlElement = configurationState.get("deployment-url-element").iterator().next();
            if (urlElement.contains("/")) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        "Deployment Url element can neither start, end, nor contain, the '/' character.");
                return false;
            }
        }

        if (isNullOrEmpty(configurationState.get("issuer-name"))) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "Issuer name must be specified.");
            return false;
        }
        //keystore state - perhaps open it?? at least has to be specified
        return true;
    }

    private void invokeRestSTSPublishService(String invocationPayload) {
        try {
            URL url = new URL(getRestSTSPublishEndpointUrl());
            HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(invocationPayload);
            writer.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                displaySuccessMessage(connection);
            } else {
                displayErrorMessage(connection);
            }
        } catch (MalformedURLException e) {
            debug.error("Exception creating rest-sts publish service url in " +
                    "RestSecurityTokenServiceViewBean#invokeRestSTSPublishService: " + e, e);
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "Exception creating rest-sts publish service url: " + e.getMessage());
        } catch (IOException e) {
            debug.error("Exception invoking rest sts publish service in " +
                    "RestSecurityTokenServiceViewBean#invokeRestSTSPublishService: " + e, e);
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "Exception invoking rest sts publish service: " + e.getMessage());
        }
    }

    private void displaySuccessMessage(HttpURLConnection connection) {
        try {
            //TODO - I18N?
            final String successMessage = readInputStream(connection.getInputStream());
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", successMessage);
        } catch (IOException e) {
            debug.error("Exception in RestSecurityTokenServiceViewBean#displaySuccessMessage: " + e, e);
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "Exception getting response from rest sts publish service : " + e.getMessage());
        }
    }

    private void displayErrorMessage(HttpURLConnection connection) {
        //TODO: not sure I should get content of input or error stream
        try {
            String errorMessage = null;
            if (connection.getErrorStream() != null) {
                errorMessage = readInputStream(connection.getErrorStream());
            } else {
                errorMessage = readInputStream(connection.getInputStream());
            }
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "Exception getting response from rest sts publish service: " + errorMessage);
        } catch (IOException e) {
            debug.error("Exception in RestSecurityTokenServiceViewBean#displayErrorMessage: " + e, e);
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    "Exception getting response from rest sts publish service: " + e.getMessage());
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

    private String getRestSTSPublishEndpointUrl() {
        return getAMDeploymentUrl() + "/rest-sts-publish/publish?_action=create";
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

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     *
     */
    public void handleButton3Request(RequestInvocationEvent event)
            throws ModelControlException, AMConsoleException {
        removePageSessionAttribute(PAGE_MODIFIED);
        AMViewBeanBase vb = getPreviousPage();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /*
    Attempt, via various means, to get the ViewBean corresponding to previous page.
     */
    private AMViewBeanBase getPreviousPage() throws AMConsoleException {
        PageTrail.Marker marker = backTrail();
        if (marker != null) {
            /*
            This branch seems never to be entered as backTrail always seems to return null. Perhaps remove? TODO:
             */
            try {
                return (AMViewBeanBase) getViewBean(Class.forName(marker.getViewBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new AMConsoleException("Could not find class corresponding to class name"
                        + marker.getViewBeanClassName() + ". Exception: " + e);
            }
        } else if (getPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME) != null) {
            /*
            This branch is entered if this ViewBean is invoked from the global configuration page
             */
            String name = (String) getPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME);
            if (name == null) {
                throw new AMConsoleException("No page session attribute corresponding to " + AMAdminConstants.SAVE_VB_NAME);
            }
            try {
                return (AMViewBeanBase) getViewBean(Class.forName(name));
            } catch (ClassNotFoundException e) {
                throw new AMConsoleException("Could not find class corresponding to class name "
                        + name + ". Exception: " + e);
            }
        } else {
            /*
            This branch is entered if an instance of the Rest SecurityTokenService is configured via the
            Access Control -> Realm -> Services (add) tab.
             */
            try {
                return (AMViewBeanBase) getViewBean(Class.forName("com.sun.identity.console.realm.ServicesViewBean"));
            } catch (ClassNotFoundException e) {
                throw new AMConsoleException("Could not find class corresponding to class name "
                        + "com.sun.identity.console.realm.ServicesViewBean" + ". Exception: " + e);
            }
        }
    }

    private boolean isNullOrEmpty(Set<String> set) {
        return ((set == null) || set.isEmpty());
    }

    public boolean isValidRealm(String token) {
        try {
            new OrganizationConfigManager(getSSOToken(), token);
        } catch (SMSException e) {
            // Cannot find realm
            return false;
        }
        return true;
    }

    /**
     * Gets the Admin SSO Token.
     *
     * @return The Admin SSO Token.
     */
    private SSOToken getSSOToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

}