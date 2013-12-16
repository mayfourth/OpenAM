package org.forgerock.openam.monitoring.cts;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;

public class CtsCRUDOperationsPerTokenTypeEntryImpl extends CtsCRUDOperationsPerTokenTypeEntry {

    private Debug debug;

    public CtsCRUDOperationsPerTokenTypeEntryImpl(SnmpMib myMib, Debug debug) {
        super(myMib);
        this.debug = debug;
    }



}
