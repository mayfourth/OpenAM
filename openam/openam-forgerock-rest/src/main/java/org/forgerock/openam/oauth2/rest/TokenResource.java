/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2014 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.oauth2.OAuthTokenStore;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.ClientDAO;
import org.restlet.Request;

import javax.inject.Inject;
import java.security.AccessController;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.REALM;
import static org.forgerock.oauth2.core.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;
import static org.forgerock.oauth2.core.OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS;

public class TokenResource implements CollectionResourceProvider {

    private final OAuth2RequestFactory<Request> requestFactory;
    private static final DateFormat DATE_FORMATTER = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM,
            DateFormat.SHORT);
    public static final String EXPIRE_TIME_KEY = "expireTime";
    private final ClientDAO clientDao;

    private final OAuthTokenStore tokenStore;

    private static SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
    private static String adminUser = SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER);
    private static AMIdentity adminUserId = null;

    static {
        if (adminUser != null) {
            adminUserId = new AMIdentity(token,
                    adminUser, IdType.USER, "/", null);
        }
    }

    private final IdentityManager identityManager;

    @Inject
    public TokenResource(OAuthTokenStore tokenStore, ClientDAO clientDao,
            OAuth2RequestFactory<Request> requestFactory, IdentityManager identityManager) {
        this.tokenStore = tokenStore;
        this.clientDao = clientDao;
        this.requestFactory = requestFactory;
        this.identityManager = identityManager;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest actionRequest, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Actions are not supported for resource instances"));
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {

        String actionId = request.getAction();

        if ("revoke".equalsIgnoreCase(actionId)) {
            if (deleteToken(context, resourceId, handler, true)) {
                handler.handleResult(json(object()));
            }
        } else {
            handler.handleError(new NotSupportedException("Action not supported."));
        }
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest createRequest, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Create is not supported for resource instances"));
    }

    /**
     * Deletes the token with the provided token id.
     *
     * @param context The context.
     * @param tokenId The token id.
     * @param handler The handler.
     * @param deleteRefreshToken Whether to delete associated refresh token, if token id is for an access token.
     * @return {@code true} if the token has been deleted.
     */
    private boolean deleteToken(ServerContext context, String tokenId, ResultHandler<?> handler,
            boolean deleteRefreshToken) {
        try {
            AMIdentity uid = getUid(context);

            JsonValue token = tokenStore.read(tokenId);
            if (token == null) {
                throw new NotFoundException("Token Not Found", null);
            }
            String username = getAttributeValue(token, USERNAME);
            if (username == null || username.isEmpty()) {
                throw new PermanentException(404, "Not Found", null);
            }

            String grantType = getAttributeValue(token, GRANT_TYPE);

            if (grantType != null && grantType.equalsIgnoreCase(CLIENT_CREDENTIALS)) {
                if (deleteRefreshToken) {
                    deleteAccessTokensRefreshToken(token);
                }
                tokenStore.delete(tokenId);
            } else {
                String realm = getAttributeValue(token, REALM);
                AMIdentity uid2 = identityManager.getResourceOwnerIdentity(username, realm);
                if (uid.equals(uid2) || uid.equals(adminUserId)) {
                    if (deleteRefreshToken) {
                        deleteAccessTokensRefreshToken(token);
                    }
                    tokenStore.delete(tokenId);
                } else {
                    throw new PermanentException(401, "Unauthorized", null);
                }
            }

            return true;

        } catch (CoreTokenException e) {
            handler.handleError(new ServiceUnavailableException(e.getMessage(), e));
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (SSOException e) {
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        } catch (IdRepoException e) {
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        } catch (UnauthorizedClientException e) {
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        }

        return false;
    }

    /**
     * Deletes the provided access token's refresh token.
     *
     * @param token The access token.
     * @throws CoreTokenException If there was a problem deleting the refresh token.
     */
    private void deleteAccessTokensRefreshToken(JsonValue token) throws CoreTokenException {
        if (OAUTH_ACCESS_TOKEN.equals(getAttributeValue(token, TOKEN_NAME))) {
            String refreshTokenId = getAttributeValue(token, REFRESH_TOKEN);
            if (refreshTokenId != null) {
                tokenStore.delete(refreshTokenId);
            }
        }
    }

    /**
     * Gets the value of the named attribute from the provided token.
     *
     * @param token The token.
     * @param attributeName The attribute name.
     * @return The attribute value.
     */
    private String getAttributeValue(JsonValue token, String attributeName) {
        final Set<String> value = getAttributeAsSet(token, attributeName);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Gets the {@code Set<String>} of values for the given attributeName.
     *
     * @param value The {@code JsonValue}.
     * @param attributeName The attribute name.
     * @return The attribute set.
     */
    @SuppressWarnings("unchecked")
    private Set<String> getAttributeAsSet(JsonValue value, String attributeName) {
        final JsonValue param = value.get(attributeName);
        if (param != null) {
            return (Set<String>) param.getObject();
        }
        return null;
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        if (deleteToken(context, resourceId, handler, false)) {
            Resource resource = new Resource(resourceId, "1", json(object(field("success", "true"))));
            handler.handleResult(resource);
        }
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Patch is not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest queryRequest, QueryResultHandler handler) {
        try {
            JsonValue response = null;
            Resource resource;
            try {
                Map<String, Object> query = new HashMap<String, Object>();
                String id = queryRequest.getQueryId();

                //get uid of submitter
                AMIdentity uid;
                try {
                    uid = getUid(context);
                    if (!uid.equals(adminUserId)) {
                        query.put(USERNAME, uid.getName());
                    } else {
                        query.put(USERNAME, "*");
                    }
                } catch (Exception e) {
                    handler.handleError(new PermanentException(401, "Unauthorized", e));
                }

                //split id into the query fields
//                String[] queries = id.split("\\,");
//                for (String q : queries) {
//                    String[] params = q.split("=");
//                    if (params.length == 2) {
//                        query.put(params[0], params[1]);
//                    }
//                }

                response = tokenStore.query(query);
            } catch (CoreTokenException e) {
                throw new ServiceUnavailableException(e.getMessage(), e);
            }
            resource = new Resource("result", "1", response);
            JsonValue value = resource.getContent();
            Set<HashMap<String, Set<String>>> list = (Set<HashMap<String, Set<String>>>) value.getObject();
            Resource res = null;
            JsonValue val = null;
            if (list != null && !list.isEmpty()) {
                for (HashMap<String, Set<String>> entry : list) {
                    val = new JsonValue(entry);
                    res = new Resource("result", "1", val);

                    val.put(EXPIRE_TIME_KEY, getExpiryDate(entry));

                    final String clientId = (String) entry.get("clientID").toArray()[0];
                    final String realm = (String) entry.get("realm").toArray()[0];

                    OAuth2Request request = new OAuth2Request() {
                        public <T> T getRequest() {
                            throw new UnsupportedOperationException("Realm parameter only OAuth2Request");
                        }

                        public <T> T getParameter(String name) {
                            if ("realm".equals(name)) {
                                return (T) realm;
                            }
                            throw new UnsupportedOperationException("Realm parameter only OAuth2Request");
                        }

                        @Override
                        public JsonValue getBody() {
                            return null;
                        }
                    };

                    Client client = clientDao.read(clientId, request);
                    String clientName = client.get(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType()).get(0).asString();
    val.put(OAuth2Constants.ShortClientAttributeNames.DISPLAY_NAME.getType(), clientName);

                    handler.handleResource(res);
                }
            }
            handler.handleResult(new QueryResult());
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (UnauthorizedClientException e) {
            e.printStackTrace();
        }
    }

    private String getExpiryDate(HashMap<String, Set<String>> entry) {
        final Long expiryTimeInMilliseconds = new Long((String) entry.get(EXPIRE_TIME_KEY).toArray()[0]);
        return DATE_FORMATTER.format(new Date(expiryTimeInMilliseconds));
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler) {

        try {
            AMIdentity uid = getUid(context);

            JsonValue response;
            Resource resource;
            try {
                response = tokenStore.read(resourceId);
            } catch (CoreTokenException e) {
                throw new NotFoundException("Token Not Found", e);
            }
            if (response == null) {
                throw new NotFoundException("Token Not Found");
            }

            String grantType = getAttributeValue(response, GRANT_TYPE);

            if (grantType != null && grantType.equalsIgnoreCase(OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS)) {
                resource = new Resource(OAuth2Constants.Params.ID, "1", response);
                handler.handleResult(resource);
            } else {
                String realm = getAttributeValue(response, REALM);

                String username = getAttributeValue(response, USERNAME);
                if (username == null || username.isEmpty()) {
                    throw new PermanentException(404, "Not Found", null);
                }
                AMIdentity uid2 = identityManager.getResourceOwnerIdentity(username, realm);
                if (uid.equals(adminUserId) || uid.equals(uid2)) {
                    resource = new Resource(OAuth2Constants.Params.ID, "1", response);
                    handler.handleResult(resource);
                } else {
                    throw new PermanentException(401, "Unauthorized", null);
                }
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (SSOException e) {
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        } catch (IdRepoException e) {
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        } catch (UnauthorizedClientException e) {
            handler.handleError(new PermanentException(401, "Unauthorized", e));
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Update is not supported for resource instances"));
    }

    /**
     * Returns TokenID from headers
     *
     * @param context ServerContext which contains the headers.
     * @return String with TokenID
     */
    private String getCookieFromServerContext(ServerContext context) {
        return RestUtils.getCookieFromServerContext(context);
    }

    private AMIdentity getUid(ServerContext context) throws SSOException, IdRepoException, UnauthorizedClientException {
        String cookie = getCookieFromServerContext(context);
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken token = mgr.createSSOToken(cookie);
        return identityManager.getResourceOwnerIdentity(token.getProperty("UserToken"), token.getProperty("Organization"));
    }

}
