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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.query;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.QueryFailedException;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.openam.cts.impl.query.reaper.ReaperQuery;
import org.forgerock.openam.cts.utils.TokenAttributeConversion;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.SimplePagedResultsControl;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fluent class responsible for constructing queries for the LDAP data store.
 *
 * This class will handle the details around preparing a query and executing the query,
 * including processing the return results.
 *
 * Uses Token as its main means of expressing the data returned from LDAP and so is
 * intended for use with the Core Token Service.
 */
public class QueryBuilder {
    // Injected
    private final Debug debug;
    private final TokenAttributeConversion attributeConversion;
    private final LDAPConfig constants;
    private final LDAPSearchHandler handler;

    private String[] requestedAttributes = new String[]{};
    private int sizeLimit;
    private Filter filter;
    private int pageSize;
    private ByteString pagingCookie;

    /**
     * Default constructor ensures the Object Class is defined.
     *
     * @param attributeConversion Required for Token based conversions.
     * @param constants Required for system wide constants.
     */
    @Inject
    public QueryBuilder(TokenAttributeConversion attributeConversion,
                        LDAPConfig constants, LDAPSearchHandler handler,
                        @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.attributeConversion = attributeConversion;
        this.constants = constants;
        this.handler = handler;
        this.debug = debug;

        sizeLimit = 0;
        pageSize = 0;
    }

    /**
     * Limit the number of results returned from this query to the given amount.
     *
     * @param maxSize Positive number, zero indicates no limit.
     * @return The QueryBuilder instance.
     */
    public QueryBuilder limitResultsTo(int maxSize) {
        sizeLimit = maxSize;
        return this;
    }

    /**
     * The search results can be paged by using the paging cookie.
     *
     * See {@link ReaperQuery} for more details on this.
     *
     * @param pageSize The size of each page of results.
     * @param cookie The paging cookie required to track paging. If this is the first
     *               call, then use #getEmptyPagingCookie to initialise the process.
     * @return The QueryBuilder instance.
     */
    public QueryBuilder pageResultsBy(int pageSize, ByteString cookie) {
        this.pageSize = pageSize;
        pagingCookie = cookie;
        return this;
    }

    /**
     * The paging cookie is used as part of Paged Search Results.
     *
     * See {@link ReaperQuery} for more details.
     *
     * @return Null if no Paging Cookie was assigned.
     */
    public ByteString getPagingCookie() {
        return pagingCookie;
    }

    /**
     * Indicates that the QueryBuilder is paging the search results, and subsequent calls are required
     * to collect the rest of the results.
     *
     * @return True if paging has been initialised, false if not.
     */
    public boolean isPagingResults() {
        return pageSize != 0;
    }

    /**
     * Limit the search to return only the named attributes.
     *
     * @param returnFields Array of CoreTokenField which are required in the results.
     * @return The QueryBuilder instance.
     * @throws IllegalArgumentException If array was null or empty.
     */
    public QueryBuilder returnTheseAttributes(CoreTokenField... returnFields) {
        Reject.ifTrue(returnFields == null || returnFields.length == 0);

        Set<String> attributes = new HashSet<String>();
        for (CoreTokenField field : returnFields) {
            attributes.add(field.toString());
        }
        return setReturnAttributes(attributes);
    }

    /**
     * Limit the search to return only the named attributes.
     *
     * @param returnFields Collection of CoreTokenField that are required in the search results.
     * @return The QueryBuilder instance.
     * @throws IllegalArgumentException If the requested fields were null or empty.
     */
    public QueryBuilder returnTheseAttributes(Collection<CoreTokenField> returnFields) {
        Reject.ifTrue(returnFields == null || returnFields.isEmpty());

        Set<String> fields = new HashSet<String>();
        for (CoreTokenField field : returnFields) {
            fields.add(field.toString());
        }
        return setReturnAttributes(fields);
    }

    private QueryBuilder setReturnAttributes(Set<String> fields) {
        requestedAttributes = fields.toArray(new String[fields.size()]);
        return this;
    }

    /**
     * Assign a filter to the query. This can be a complex filter and is handled
     * by the QueryFilter class.
     *
     * @see QueryFilter For more details on generating a filter.
     *
     * @param filter An OpenDJ SDK Filter to assign to the query.
     * @return The QueryBuilder instance.
     */
    public QueryBuilder withFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    private Filter getLDAPFilter() {
        Filter objectClass = Filter.equality(CoreTokenConstants.OBJECT_CLASS, CoreTokenConstants.FR_CORE_TOKEN);
        if (filter == null) {
            return objectClass;
        } else {
            return Filter.and(objectClass, filter);
        }
    }

    /**
     * Perform an Attribute based query against the persistent store.
     *
     * An attribute query is one where we are interested in specific attributes from the Tokens
     * rather than all attributes assigned to the matched Tokens. This can be more performant
     * than requesting back all attributes of a Token first.
     *
     * @param connection The connection used to perform the request.
     *
     * @return A non null, but possibly empty collection of PartialTokens.
     *
     * @throws CoreTokenException If there was an error performing the query or in converting
     * the results.
     */
    public Collection<PartialToken> executeAttributeQuery(Connection connection) throws CoreTokenException {
        Collection<Entry> entries = executeRawResults(connection);
        Collection<PartialToken> partialTokens = new ArrayList<PartialToken>(entries.size());
        for (Entry entry : entries) {
            partialTokens.add(new PartialToken(attributeConversion.mapFromEntry(entry)));
        }
        return partialTokens;
    }

    /**
     * Perform the query and return the results as Entry instances.
     *
     * @param connection The connection used to perform the request.
     *
     * @return A non null but possibly empty collection.
     *
     * @throws QueryFailedException If there was an error during the query.
     */
    public Collection<Entry> executeRawResults(Connection connection) throws CoreTokenException {
        // Prepare the search
        Filter ldapFilter = getLDAPFilter();
        SearchRequest searchRequest = Requests.newSearchRequest(
                constants.getTokenStoreRootSuffix(),
                SearchScope.WHOLE_SUBTREE,
                ldapFilter,
                requestedAttributes);
        searchRequest.setSizeLimit(sizeLimit);

        if (isPagingResults()) {
            searchRequest = searchRequest.addControl(SimplePagedResultsControl.newControl(true, pageSize, pagingCookie));
        }

        // Perform the search
        Collection<Entry> entries = createResultsList();
        final Result result = handler.performSearch(connection, searchRequest, entries);

        if (isPagingResults()) {
            try {
                SimplePagedResultsControl control = result.getControl(
                        SimplePagedResultsControl.DECODER, new DecodeOptions());
                pagingCookie = control.getCookie();
            } catch (DecodeException e) {
                throw new CoreTokenException("Failed to decode Paging Cookie", e);
            }
        }

        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Query: matched {0} results\n" +
                    "Search Request: {1}\n" +
                    "Filter: {2}\n" +
                    "Result: {3}",
                    entries.size(),
                    searchRequest,
                    ldapFilter.toString(),
                    result));
        }

        return entries;
    }

    /**
     * @return Creates a list based on the state of the builder.
     */
    private Collection<Entry> createResultsList() {
        Collection<Entry> entries;
        if (isPagingResults()) {
            entries = new ArrayList<Entry>(pageSize);
        } else if (sizeLimit != 0) {
            entries = new ArrayList<Entry>(sizeLimit);
        } else {
            entries = new ArrayList<Entry>();
        }
        return entries;
    }

    /**
     * Perform the query and return the results as processed Token instances.
     *
     * @param connection The connection used to perform the request.
     *
     * @return A non null but possibly empty collection.
     *
     * @throws CoreTokenException If there was an error during the query.
     */
    public Collection<Token> execute(Connection connection) throws CoreTokenException {
        if (!ArrayUtils.isEmpty(requestedAttributes)) {
            throw new IllegalStateException(
                    "Cannot convert results to Token if the query uses" +
                    "a reduced number of attributes in the return result");
        }

        Collection<Entry> results = executeRawResults(connection);
        List<Token> tokens = new ArrayList<Token>(results.size());
        for (Entry entry : results) {
            Token token = attributeConversion.tokenFromEntry(entry);
            tokens.add(token);
        }

        if (debug.messageEnabled()) {

            String msg = "";
            String separator = "\n";

            for (Token t : tokens) {
                msg += t + separator;
                // Ensure we don't fill up the logs
                if (msg.length() > 500) break;
            }

            if (msg.endsWith(separator)) {
                msg = msg.substring(0, msg.length() - separator.length());
            }

            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Query: Matched {0}, some Tokens are shown below:\n" +
                    "{1}",
                    tokens.size(),
                    msg));
        }
        return tokens;
    }

    /**
     * Presents the QueryBuilder in a human readable format.
     *
     * Note: This function is not performant and should only be used for debugging purposes.
     *
     * @return Non null string representing this QueryBuilder.
     */
    @Override
    public String toString() {
        return MessageFormat.format(
                "Query:\n" +
                "      Filter: {0}\n" +
                "  Attributes: {1}",
                getLDAPFilter(),
                StringUtils.join(requestedAttributes, ", "));
    }

    /**
     * Generates an empty Paging Cookie.
     *
     * The Paging Cookie is required for paging requests. In order to use the
     * paging function, an empty cookie is passed in initially and must be
     * provided with each subsequent call.
     *
     * The details of this are managed by the ReaperIterator.
     *
     * @see ReaperQuery
     *
     * @return Non null empty ByteString.
     */
    public static ByteString getEmptyPagingCookie() {
        return ByteString.empty();
    }
}
