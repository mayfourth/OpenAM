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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import com.sun.identity.common.HttpURLConnectionManager;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolverFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see org.forgerock.openam.authentication.modules.oidc.OpenIdResolverCache
 */
public class OpenIdResolverCacheImpl implements OpenIdResolverCache {
    private final OpenIdResolverFactory openIdResolverFactory;
    private final ConcurrentHashMap<String, OpenIdResolver> resolverMap;

    @Inject
    OpenIdResolverCacheImpl(OpenIdResolverFactory openIdResolverFactory) {
        this.openIdResolverFactory = openIdResolverFactory;
        resolverMap = new ConcurrentHashMap<String, OpenIdResolver>();
    }

    public OpenIdResolver getResolverForIssuer(String cryptoContextDefinitionValue) {
        return resolverMap.get(cryptoContextDefinitionValue);
    }

    /**
    It is possible that two callers are calling this method at once. I want to leverage the uncontested reads
    of the ConcurrentHashMap, and I don't want to synchronize the writes to the ConcurrentHashMap above the
    synchronization applied by the CHM in puts. The drawback of this approach is the possible redundant creation of
    a OpenIdResolver if two concurrent calls target the currently-uncreated OpenIdResolver, but the redundant creation will
    only occur once.
     @see org.forgerock.openam.authentication.modules.oidc.OpenIdResolverCache
     */
    @Override
    public OpenIdResolver createResolver(String issuerFromJwk, String cryptoContextType, String cryptoContextValue,
                                         URL cryptoContextValueUrl) throws FailedToLoadJWKException {
        OpenIdResolver newResolver = null;
        if (OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CLIENT_SECRET.equals(cryptoContextType)) {
            newResolver = openIdResolverFactory.createSharedSecretResolver(issuerFromJwk, cryptoContextValue);
        } else if (OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL.equals(cryptoContextType)) {
            newResolver = openIdResolverFactory.createFromOpenIDConfigUrl(cryptoContextValueUrl);
            //check is only relevant in this block, as issuer is specified in the json blob referenced by url.
            if (!issuerFromJwk.equals(newResolver.getIssuer())) {
                throw new IllegalStateException("The specified issuer, " + issuerFromJwk + ", does not match the issuer, "
                        + newResolver.getIssuer() + " referenced by the configuration url, " + cryptoContextValue);
            }
        } else if (OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_JWK_URL.equals(cryptoContextType)) {
            newResolver = openIdResolverFactory.createJWKResolver(issuerFromJwk, cryptoContextValueUrl,
                    HttpURLConnectionManager.getReadTimeout(),
                    HttpURLConnectionManager.getConnectTimeout());
        } else {
            /*
            Should not enter this block, as the cryptoContextType was validated to be of the three expected types in
            OpenIdModule.init, but all bases should be covered. This exception is not caught by the OpenIdConnect caller.
             */
            throw new IllegalArgumentException("The specified cryptoContextType, " + cryptoContextType + " was unexpected!");
        }
        OpenIdResolver oldResolver = null;
        if ((oldResolver = resolverMap.putIfAbsent(cryptoContextValue, newResolver)) != null) {
            return oldResolver;
        }
        return newResolver;
    }
}
