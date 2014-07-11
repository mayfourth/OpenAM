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

import java.util.Map;

public class RestSecurityTokenServiceViewBean extends AMServiceProfileViewBeanBase {
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
         */
        submitCycle = true;
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
        forwardTo();
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
