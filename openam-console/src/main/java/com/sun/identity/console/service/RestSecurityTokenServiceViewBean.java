package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.web.ui.view.alert.CCAlert;
import org.forgerock.json.fluent.JsonValue;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

public class RestSecurityTokenServiceViewBean extends AMServiceProfileViewBeanBase {
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
    public static final String REST_STS_PUBLISH_INSTANCE_STATE = "instance_state";


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
    public void handleButton1Request(RequestInvocationEvent event)
            throws ModelControlException {
        /*
        This is the code directly from the superclass. Presumably it saves state in the sms ? I need to
        marshal a RestSTSInstanceConfig instance from the attributes and call the
        rest sts publish service here...
        Also I need to set the amDeploymentUrl TODO
         */
        submitCycle = true;
        Map<String, Set<String>> configurationState = (Map<String, Set<String>>)getAttributeValues();
        JsonValue propertiesMap = new JsonValue(marshalSetValuesToListValues(configurationState));
        JsonValue invocationJson = json(object(
                field(REST_STS_PUBLISH_INVOCATION_CONTEXT, REST_STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN),
                field(REST_STS_PUBLISH_INSTANCE_STATE, propertiesMap)));
        try {
            Representation representation = new ClientResource(getRestSTSPublishEndpointUrl())
                    .post(new StringRepresentation(invocationJson.toString(), MediaType.APPLICATION_JSON));
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    representation.getText()); //TODO - create an I18N message that just displays success and url fragment
        } catch (ResourceException re) {
            //the Restlet ResourceException only displays status in toString method.
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", re.getMessage());
        } catch (Exception e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.toString());
        }
/*
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();

        if (model != null) {
            try {
                Map values = getValues();
                onBeforeSaveProfile(values);
                model.setAttributeValues(values);

                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "message.updated");
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
            }
        }
 */
        forwardTo();
    }

    private String getRestSTSPublishEndpointUrl() {
        //TODO: proper endpoint
        return "http://localhost:8080/openam/rest-sts-publish/publish?_action=create";
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
    public void handleButton3Request(RequestInvocationEvent event) {
    returnToMainPage();
    }
     */
    public void handleButton3Request(RequestInvocationEvent event)
            throws ModelControlException, AMConsoleException {
        removePageSessionAttribute(PAGE_MODIFIED);
        backTrail();
        try {
            String name = (String) getPageSessionAttribute(
                    AMAdminConstants.SAVE_VB_NAME);
            SCConfigViewBean vb = (SCConfigViewBean) getViewBean(
                    Class.forName(name));
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            debug.warning(
                    "RestSecurityTokenServiceViewBean.handleButton3Request:", e);
        }
    }

}
