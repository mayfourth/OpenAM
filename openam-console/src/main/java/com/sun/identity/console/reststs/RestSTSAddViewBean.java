package com.sun.identity.console.reststs;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.PageTrail;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.reststs.model.RestSTSModel;
import com.sun.identity.console.reststs.model.RestSTSModelImpl;
import com.sun.identity.console.reststs.model.RestSTSModelResponse;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

import javax.servlet.http.HttpServletRequest;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;

public class RestSTSAddViewBean extends AMPrimaryMastHeadViewBean {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/reststs/RestSTSAdd.jsp";

    public static final String PAGE_MODIFIED = "pageModified";
    private static final String PGTITLE = "pgtitle";
    public static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;

    public RestSTSAddViewBean() {
        super("RestSTSAdd");
    }

    protected void initialize() {
        if (!initialized) {
            super.initialize();
            createPropertyModel();
            createPageTitleModel();
            registerChildren();
            initialized = true;
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(PGTITLE, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        if (propertySheetModel != null) {
            propertySheetModel.registerChildren(this);
        }
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if ((propertySheetModel != null) &&
                propertySheetModel.isChildSupported(name)
                ) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/threeBtnsPageTitle.xml"));

        ptModel.setPageTitleText("TODO");//TODO
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel("TODO")); //TODO
    }

    protected void createPropertyModel() {
        String xmlFileName = "com/sun/identity/console/propertyRestSecurityTokenService.xml";
        String xml = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xmlFileName));

        propertySheetModel = new AMPropertySheetModel(xml);
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        try {
            return new RestSTSModelImpl(req, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
            throw new IllegalStateException("Exception getting model in RestSTSAddViewBean: " + e.getMessage(), e);
        }
    }

    /**
     * Handles save button request. Validates the rest sts configuration state, and invokes the model to publish a
     * rest sts instance corresponding to this state.
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) throws ModelControlException {
        Map<String, Set<String>> configurationState = (Map<String, Set<String>>)getAttributeSettings();
        RestSTSModel model = (RestSTSModel)getModel();
        RestSTSModelResponse validationResponse = model.validateConfigurationState(configurationState);
        if (validationResponse.isSuccessful()) {
            final String currentRealm = (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
            try {
                RestSTSModelResponse creationResponse = model.createInstance(configurationState, currentRealm);
                if (creationResponse.isSuccessful()) {
                    setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", creationResponse.getMessage());
                } else {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", creationResponse.getMessage());
                }
            } catch (AMConsoleException e) {
                e.printStackTrace();
            }
        } else {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", validationResponse.getMessage());
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
}
