package org.forgerock.openam.monitoring.cts;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;

public class CtsCRUDOperationsEntryImpl extends CtsCRUDOperationsEntry {

    private Debug debug;

    public CtsCRUDOperationsEntryImpl(SnmpMib myMib, Debug debug) {
        super(myMib);
        this.debug = debug;
    }


}
