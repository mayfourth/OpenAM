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

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;
import java.util.Collection;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.QueryBuilder;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.impl.query.QueryFilter;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;

import javax.inject.Named;

/**
 * Implementation of the CTSTokenOperationsEntry.
 *
 * These are operations which take only as an arguemtn the token type.
 *
 * For example, querying for the total number of a given type of token in the CTS at
 * any given moment.
 *
 * These are operations which only take a token type as the input.
 */
public class CtsTokenOperationsEntryImpl extends CtsTokenOperationsEntry {

    //on error
    private final Debug debug;

    //for building up our query
    private final QueryFactory factory;

    private final static Long ERROR = -1l;

    /**
     * Constructor allows us to pass in the QueryFactory as well as the
     * appropriate debug system.
     *
     * @param factory Factory used to connect and query the CTS
     * @param myMib Mib file this Entry implementation is a member of
     */
    public CtsTokenOperationsEntryImpl(QueryFactory factory, SnmpMib myMib,
                                       @Named(CoreTokenConstants.CTS_MONITOR_DEBUG) Debug debug) {
        super(myMib);
        this.factory = factory;
        this.debug = debug;
    }

    /**
     * Returns the total number of current tokens of the type specified
     * in the CTS at the current instant.
     *
     * @return a long of the number of tokens in the store, min. 0.
     */
    @Override
    public Long getTotalCount() {
        final long result;
        final TokenType token;

        try {
            // -1 as our indexes start at 1, instead of 0.
            token = getTokenFromOrdinalIndex( getTokenTableIndex().intValue() - 1 );
        } catch (SnmpStatusException e) {
            if (debug.messageEnabled()) {
                debug.error("Unable to determine token type from supplied index.");
            }
            return ERROR;
        }

        if (token == null) {
            if (debug.messageEnabled()) {
                debug.error("Token type returned was null. Check supplied index.");
            }
            return ERROR;
        }

        //create the filter to restrict by token type
        final QueryFilter.QueryFilterBuilder filterBuilder = factory.createFilter().and();
        filterBuilder.attribute(CoreTokenField.TOKEN_TYPE, token);

        final Filter filter = filterBuilder.build();

        //create the query itself
        final QueryBuilder builder = factory.createInstance();
        builder.returnTheseAttributes(CoreTokenField.TOKEN_ID);
        builder.withFilter(filter);

        try {

            Collection<Entry> results = builder.executeRawResults();
            result = results.size();

        } catch (CoreTokenException e) {
            if (debug.messageEnabled()) {
                debug.error("CTS Persistence did not return a valid result.");
            }

            return ERROR;
        }

        return result;
    }

    /**
     * Retrieves the appropriate TokenType from the list of avaliable
     * enums that matches on the ordinal index.
     *
     * @param ordinalIndex the ordinal index to look up
     * @return the TokenType this ordinal value represents, null otherwise
     */
    private TokenType getTokenFromOrdinalIndex(int ordinalIndex) {

        if (ordinalIndex < 0 || ordinalIndex > TokenType.values().length) {
            return null;
        }

        return TokenType.values()[ordinalIndex];
    }

    /**
     * Exposing a public setter for ease of testing
     */
    public void setTokenTableIndex(long index) {
        this.TokenTableIndex = index;
    }

}