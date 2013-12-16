package org.forgerock.openam.monitoring.cts;

import java.io.Serializable;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.forgerock.openam.cts.api.TokenType;

/**
 * The class is used for representing "SUN-OPENSSO-SERVER-MIB".
 * You can edit the file if you want to modify the behavior of the MIB.
 */
public class FORGEROCK_OPENAM_CTS_MIBImpl extends FORGEROCK_OPENAM_CTS_MIB implements Serializable {

    //CTS
    private CtsMonitoringImpl<OperationType, TokenType> ctsMonitoringGroup;

    /**
     * Default constructor. Initialize the Mib tree.
     */
    public FORGEROCK_OPENAM_CTS_MIBImpl() {
    }

    // ------------------------------------------------------------
    //
    // Initialization of the "SsoServerCTSMonitoring" group.
    //
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerTopology" group MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("SsoServerTopology")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "SsoServerTopology" group (SsoServerTopology)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerTopologyMBean"
     * interface.
     **/
    @Override
    protected Object createCtsMonitoringMBean(
            String groupName,
            String groupOid,
            ObjectName groupObjname,
            MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerCTSMonitoringMBean"
        // interface.
        //
        if (server != null)
            ctsMonitoringGroup = new CtsMonitoringImpl<OperationType, TokenType>(this, server,
                    OperationType.class, TokenType.class);
        else
            ctsMonitoringGroup = new CtsMonitoringImpl<OperationType, TokenType>(this,
                    OperationType.class, TokenType.class);

        return ctsMonitoringGroup;
    }

    public CtsMonitoringImpl getCtsMonitoringGroup() {
        return ctsMonitoringGroup;
    }

}
