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
import com.sun.management.snmp.agent.SnmpMib;
import java.util.ArrayList;
import static org.fest.assertions.Fail.fail;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.QueryBuilder;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.impl.query.QueryFilter;
import org.forgerock.opendj.ldap.Entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CtsTokenOperationsEntryImplTest {

    private CtsTokenOperationsEntryImpl entryImpl;

    private QueryFactory factory;
    private Debug debug;

    @BeforeMethod
    public void setUp() {

        factory = mock(QueryFactory.class);
        debug = mock(Debug.class);

        SnmpMib myMib = mock(SnmpMib.class);

        entryImpl = new CtsTokenOperationsEntryImpl(factory, myMib, debug);
    }

    @Test
    public void getTotalCountTest() {
        //given
        entryImpl.setTokenTableIndex(1l); // 1 -> 0 because of different indexes

        TokenType token = TokenType.values()[0];
        ArrayList<Entry> results = new ArrayList<Entry>();

        Entry mockEntry = mock(Entry.class);
        results.add(mockEntry);

        QueryBuilder queryBuilder = mock(QueryBuilder.class);
        QueryFilter.QueryFilterBuilder queryFilterBuilder = mock(QueryFilter.QueryFilterBuilder.class);
        QueryFilter queryFilter = mock(QueryFilter.class);

        given(factory.createInstance()).willReturn(queryBuilder);

        given(factory.createFilter()).willReturn(queryFilter);
        given(queryFilter.and()).willReturn(queryFilterBuilder);

        try {
            given(queryBuilder.executeRawResults()).willReturn(results);
        } catch (CoreTokenException e) {
            fail();
        }

        //when
        long result = entryImpl.getTotalCount();

        //then
        verify(queryFilterBuilder).attribute(CoreTokenField.TOKEN_TYPE, token);
        verify(queryBuilder).returnTheseAttributes(CoreTokenField.TOKEN_ID);

        assertEquals(1l, result);
    }

    @Test
    public void getTotalCountTestErrorInvalidToken() {
        //given
        entryImpl.setTokenTableIndex(-1l);

        //when
        long result = entryImpl.getTotalCount();

        //then
        verify(debug, times(1)).messageEnabled();
        assertEquals(-1l, result);

    }

    @Test
    public void getTotalCountTestErrorPersistentStoreFail() {
        //given
        entryImpl.setTokenTableIndex(1l); // 1 -> 0 because of different indexes

        TokenType token = TokenType.values()[0];

        QueryBuilder queryBuilder = mock(QueryBuilder.class);
        QueryFilter.QueryFilterBuilder queryFilterBuilder = mock(QueryFilter.QueryFilterBuilder.class);
        QueryFilter queryFilter = mock(QueryFilter.class);

        given(factory.createInstance()).willReturn(queryBuilder);

        given(factory.createFilter()).willReturn(queryFilter);
        given(queryFilter.and()).willReturn(queryFilterBuilder);

        try {
            given(queryBuilder.executeRawResults()).willThrow(new CoreTokenException("Error"));
        } catch (CoreTokenException e) {
            fail();
        }

        //given
        long result = entryImpl.getTotalCount();

        //then
        verify(queryFilterBuilder).attribute(CoreTokenField.TOKEN_TYPE, token);
        verify(queryBuilder).returnTheseAttributes(CoreTokenField.TOKEN_ID);

        assertEquals(-1l, result);
    }

}
