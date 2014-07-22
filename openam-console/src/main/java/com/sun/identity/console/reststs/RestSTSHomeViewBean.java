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
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.realm.RealmPropertiesBase;
import com.sun.identity.console.realm.ServicesEditViewBean;
import com.sun.identity.console.realm.model.ServicesModel;
import com.sun.identity.console.reststs.model.RestSTSModel;
import com.sun.identity.console.reststs.model.RestSTSModelImpl;
import com.sun.identity.console.service.model.SCUtils;
import com.sun.identity.shared.locale.Locale;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

public class RestSTSHomeViewBean extends RealmPropertiesBase {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/reststs/RestSTSHome.jsp";
//    private static final String CHILD_TBL_TILED_VIEW = "tableTiledView";
//    private static final String TBL_SEARCH = "tblSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
//    private static final String TF_DATA_NAME = "tfDataName";
    private static final String TBL_COL_ACTION = "tblColAction";
    private static final String TBL_DATA_ACTION_LABEL = "tblDataActionLabel";
    private static final String PAGETITLE = "pgtitle";
    static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";

    private CCActionTableModel tblModel = null;

    /**
     * Creates a services view bean.
     */
    public RestSTSHomeViewBean() {
        super("Rest STS");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createTableModel();
        createPageTitleModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PAGETITLE, CCPageTitle.class);
//        registerChild(TBL_SEARCH, CCActionTable.class);
        ptModel.registerChildren(this);
        tblModel.registerChildren(this);
//        registerChild(CHILD_TBL_TILED_VIEW, ServicesTiledView.class);
    }

    protected View createChild(String name) {
        View view = null;
/*
        if (name.equals(CHILD_TBL_TILED_VIEW)) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            populateTableModel((Map)szCache.getSerializedObj());
            view = new ServicesTiledView(this, tblModel, name);
        } else if (name.equals(TBL_SEARCH)) {
            ServicesTiledView tView = (ServicesTiledView)getChild(
                    CHILD_TBL_TILED_VIEW);
            CCActionTable child = new CCActionTable(this, tblModel, name);
            child.setTiledView(tView);
            view = child;
        } else*/ if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this,name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
            throws ModelControlException {
        super.beginDisplay(event);
        resetButtonState(TBL_BUTTON_DELETE);
        setRestSTSInstanceNamesInTable();
        setPageTitle(getModel(), "reststs.home.page.title");
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new RestSTSModelImpl(req, getPageSessionAttributes());
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", getBackButtonLabel());
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/tblRestSTSInstances.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "reststs.home.instances.table.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
                "reststs.home.instances.table.button.delete");
        tblModel.setActionValue(TBL_COL_NAME,
                "reststs.home.instances.table.column.name");
        tblModel.setActionValue(TBL_COL_ACTION,
                "reststs.home.instances.table.action.column.name");
    }

    private void setRestSTSInstanceNamesInTable() {
        RestSTSModel model = (RestSTSModel)getModel();

        try {
            String curRealm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
            populateTableModel(model.getPublishedInstances(curRealm));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
    }

    private void populateTableModel(Set<String> publishedInstances) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if (!publishedInstances.isEmpty()) {
            boolean firstEntry = true;
            for (String instanceName : publishedInstances) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }

                tblModel.setValue(TBL_DATA_NAME, instanceName);
                tblModel.setValue(TBL_DATA_ACTION_HREF, instanceName);
//                tblModel.setValue(TBL_DATA_ACTION_LABEL,
//                        "table.services.action.edit");
            }
            szCache.setValue((Serializable)publishedInstances);
        } else {
            szCache.setValue(null);
        }
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
        RestSecurityTokenServiceViewBean vb = (RestSecurityTokenServiceViewBean)getViewBean(
                RestSecurityTokenServiceViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to edit Service view bean.
     *
     * @param serviceName name of the service to be edited
     */
    public void handleTblDataActionHrefRequest(String serviceName) {
        ServicesModel model = (ServicesModel)getModel();

        SCUtils utils = new SCUtils(serviceName, model);
        String propertiesViewBeanURL = utils.getServiceDisplayURL();

        if ((propertiesViewBeanURL != null) &&
                (propertiesViewBeanURL.trim().length() > 0)
                ) {
            String curRealm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
            if (curRealm == null) {
                curRealm = AMModelBase.getStartDN(
                        getRequestContext().getRequest());
            }

            try {
                String pageTrailID = (String)getPageSessionAttribute(
                        PG_SESSION_PAGE_TRAIL_ID);
                propertiesViewBeanURL += "?ServiceName=" + serviceName +
                        "&Location=" +
                        Locale.URLEncodeField(curRealm, getCharset(model)) +
                        "&Template=true&Op=" + AMAdminConstants.OPERATION_EDIT +
                        "&" + PG_SESSION_PAGE_TRAIL_ID + "=" + pageTrailID;
                HttpServletResponse response =
                        getRequestContext().getResponse();
                response.sendRedirect(propertiesViewBeanURL);
            } catch (UnsupportedEncodingException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
                forwardTo();
            } catch (IOException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
                forwardTo();
            }
        } else {
            ServicesEditViewBean vb = (ServicesEditViewBean)getViewBean(
                    ServicesEditViewBean.class);
            setPageSessionAttribute(ServicesEditViewBean.SERVICE_NAME,
                    serviceName);
            // set save vb to return to this view after selecting back
            // button in services edit viewbean.
            setPageSessionAttribute(
                    AMAdminConstants.SAVE_VB_NAME, getClass().getName());
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }

    public void handleTblButtonDeleteRequest(RequestInvocationEvent event) throws ModelControlException {
//        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
//        table.restoreStateData();

        Integer[] selected = tblModel.getSelectedRows();
        Set<String> instanceNames = new HashSet<String>(selected.length);

        for (int i = 0; i < selected.length; i++) {
            tblModel.setRowIndex(selected[i].intValue());
            instanceNames.add((String)tblModel.getValue(TBL_DATA_NAME));
        }

        try {
            RestSTSModel model = (RestSTSModel)getModel();
            String curRealm = (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
            model.deleteInstances(curRealm, instanceNames);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "reststs.home.instance.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "reststs.home.instances.deleted");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }

        forwardTo();
    }

    /**
     * Handles "back to" page request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        backTrail();
        forwardToRealmView(event);
    }
}
