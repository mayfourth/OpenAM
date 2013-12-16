/*
 * Copyright 2013 ForgeRock AS.
 *
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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 */

package org.forgerock.openam.monitoring.cts;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.management.MBeanServer;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.guice.InjectorHolder;

/**
 * The class extends the "SsoServerCTSMonitoring" class.
 */
public class CtsMonitoringImpl<E extends Enum<E>, F extends Enum<F>> extends CtsMonitoring {

    private static Debug debug = null;

    //our two major indexes
    private EnumSet<E> crudItems;
    private EnumSet<F> tokenItems;

    /**
     * Constructors
     */
    public CtsMonitoringImpl(SnmpMib myMib, Class<E> crudItemClass, Class<F> tokenItemClass) {
        super(myMib);
        setCrudItems(crudItemClass);
        setTokenItems(tokenItemClass);
        init(myMib, null);
    }

    public CtsMonitoringImpl(SnmpMib myMib, MBeanServer server, Class<E> crudItemClass, Class<F> tokenItemClass) {
        super(myMib);
        setCrudItems(crudItemClass);
        setTokenItems(tokenItemClass);
        init(myMib, server);
    }

    //set up our internal state before we process them
    private void setCrudItems(Class<E> crudType) {
        this.crudItems = EnumSet.allOf(crudType);
    }

    private void setTokenItems(Class<F> tokenType) {
        this.tokenItems = EnumSet.allOf(tokenType);
    }

    /**
     * Perofrms the majority of the work setting up the tables
     * such that they can be queried as pass-throughs to the
     * data structures they act as queryable endpoints for.
     *
     * @param myMib The mib file which caused generation of this class
     * @param server The MBean server (if applicable)
     */
    private void init(SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            final Key<Debug> key = Key.get(Debug.class, Names.named(CoreTokenConstants.CTS_MONITOR_DEBUG));
            debug = InjectorHolder.getInstance(key);
        }

        //init our index tables first
        final List<OperationEntry> operationEntries = new ArrayList<OperationEntry>();
        final List<TokenEntry> tokenEntries = new ArrayList<TokenEntry>();

        //init our CRUD operations
        for (Enum e : crudItems) {
            final OperationEntry entry = new OperationEntry(myMib);
            entry.OperationType = e.name();
            entry.OperationTableIndex = (long) e.ordinal() + 1;

            operationEntries.add(entry);
        }

        //init our tokens
        for (Enum e : tokenItems) {
            final TokenEntry entry = new TokenEntry(myMib);
            entry.TokenType = e.name();
            entry.TokenTableIndex = (long) e.ordinal() + 1;

            tokenEntries.add(entry);
        }

        try {
            for (OperationEntry ce : operationEntries) {
                OperationTable.addEntry(ce);
            }

            for (TokenEntry te : tokenEntries) {
                TokenTable.addEntry(te);
            }

            //init the underlying tables
            createCRUDOperationsPerTokenTypeTable(myMib, CtsCRUDOperationsPerTokenTypeTable,
                    operationEntries, tokenEntries);
            createCRUDOperationsTable(myMib, CtsCRUDOperationsTable, operationEntries);
            createTokenOperationsTable(myMib, CtsTokenOperationsTable, tokenEntries);

        } catch (SnmpStatusException e) {
            if(debug.messageEnabled()) {
                debug.error("Unable to set up CTS Monitoring tables. CTS monitoring not available.");
            }
        }

    }

    /**
     * Generates the endpoints for the CRUD Operations table.
     * @param myMib Mibfile from which the definition of these tables comes
     * @param table The table in to which we will write the endpoints
     * @param tokenEntries The entries from which the table will be populated
     * @throws SnmpStatusException If anything goes wrong while writing to the table
     */
    private void createTokenOperationsTable(SnmpMib myMib, TableCtsTokenOperationsTable table,
                                            List<TokenEntry> tokenEntries) throws SnmpStatusException {

        final QueryFactory factory = new QueryFactory();

        for (TokenEntry te : tokenEntries) {
            final CtsTokenOperationsEntry entry = new CtsTokenOperationsEntryImpl(factory, myMib, debug);
            entry.TokenTableIndex = te.TokenTableIndex;

            table.addEntry(entry);
        }

    }

    /**
     * Generates the endpoints for the CRUD Operations table.
     *
     * @param myMib Mibfile from which the definition of these tables comes
     * @param table Tghe table in to which we will write the endpoints
     * @param operationEntries The entries from which the table will be populated
     * @throws SnmpStatusException If anything goes wrong while writing to the table
     */
    private void createCRUDOperationsTable(SnmpMib myMib, TableCtsCRUDOperationsTable table,
                                           List<OperationEntry> operationEntries) throws SnmpStatusException {

        for (OperationEntry oe : operationEntries) {
              final CtsCRUDOperationsEntry entry = new CtsCRUDOperationsEntryImpl(myMib, debug);
              entry.OperationTableIndex = oe.OperationTableIndex;

              table.addEntry(entry);
        }

    }

    /**
     * Generates the endpoints for the CRUD Operations Per Token Type table.
     *
     * @param myMib Mibfile from which the definition of these tables comes
     * @param table Tghe table in to which we will write the endpoints
     * @param operationEntries The operation entries from which the table will be populated
     * @param tokenEntries The token entries from which the table will be populated
     * @throws SnmpStatusException If anything goes wrong while writing to the table
     */
    private void createCRUDOperationsPerTokenTypeTable(SnmpMib myMib, TableCtsCRUDOperationsPerTokenTypeTable table,
                                                       List<OperationEntry> operationEntries,
                                                       List<TokenEntry> tokenEntries) throws SnmpStatusException {

        for (OperationEntry oe : operationEntries) {
            for (TokenEntry te : tokenEntries) {
                final CtsCRUDOperationsPerTokenTypeEntry entry = new CtsCRUDOperationsPerTokenTypeEntryImpl(myMib, debug);
                entry.OperationTableIndex = oe.OperationTableIndex;
                entry.TokenTableIndex = te.TokenTableIndex;

                table.addEntry(entry);
            }
        }

    }

}

