package com.sun.identity.console.reststs;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMTableTiledView;
import com.sun.web.ui.model.CCActionTableModel;

public class RestSTSTiledView extends AMTableTiledView {

    public RestSTSTiledView(View parent, CCActionTableModel model, String name) {
        super(parent, model, name);
    }

    public void handleTblDataActionHrefRequest(RequestInvocationEvent event) {
        String name = (String) getDisplayFieldValue(
                RestSTSHomeViewBean.TBL_DATA_ACTION_HREF);
        RestSTSHomeViewBean parentViewBean = (RestSTSHomeViewBean) getParentViewBean();
        parentViewBean.handleTblDataActionHrefRequest(name);
    }
}
