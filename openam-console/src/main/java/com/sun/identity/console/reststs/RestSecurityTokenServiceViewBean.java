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

package com.sun.identity.console.reststs;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
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
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.web.ui.view.alert.CCAlert;

import javax.servlet.http.HttpServletRequest;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;

public class RestSecurityTokenServiceViewBean extends AMServiceProfileViewBeanBase {
    private static final Debug debug = Debug.getInstance("sts");

    public static final String DEFAULT_DISPLAY_URL =
            "/console/reststs/RestSecurityTokenService.jsp";

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

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new RestSTSModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Handles save button request. Validates the rest sts configuration state, and invokes the model to publish a
     * rest sts instance corresponding to this state.
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) throws ModelControlException {
        submitCycle = true;
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