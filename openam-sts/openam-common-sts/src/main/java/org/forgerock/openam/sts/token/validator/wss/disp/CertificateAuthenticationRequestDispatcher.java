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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator.wss.disp;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import com.google.inject.Inject;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.restlet.data.MediaType;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import java.net.URI;

import org.restlet.util.Series;
import org.slf4j.Logger;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Class which encapsulates knowledge as to how to post a x509 certificate to the OpenAM REST authN context. This class
 * will initiate the authN process to receive the json callback with a placeholder for an X509Certificate, and set this
 * reference, and return the json callback state.
 */
public class CertificateAuthenticationRequestDispatcher implements TokenAuthenticationRequestDispatcher<X509Certificate[]> {
    private final Logger logger;

    @Inject
    public CertificateAuthenticationRequestDispatcher(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Representation dispatch(URI uri, AuthTargetMapping.AuthTarget target, X509Certificate[] certificates) throws TokenValidationException {
        //TODO: log if there is more than one cert - or do I just dispatch multiple requests, or ??  - for now, just log
        //and how do I put multiple certs in the callback - just encode the entire chain?? It looks like common practice is just to use the
        //first element in the array...Confirm this - seems to make sense, as it will be the leaf cert, and all other certs in the chain
        //should be in the recipient's trust store.
        if (certificates.length > 1) {
            StringBuilder stringBuilder = new StringBuilder("Dealing with more than a single certificate. Their DNs:");
            for (int ndx = 0; ndx < certificates.length; ndx++) {
                stringBuilder.append("\n").append(certificates[ndx].getSubjectDN());
            }
            logger.warn(stringBuilder.toString());
        }
        final JsonValue callbackState = obtainCallbackState(uri);
        final JsonValue fulfilledCallback = setCertificateInCallback(callbackState, certificates[0]);
        return postFulfilledCallback(fulfilledCallback, uri);
    }

    private JsonValue obtainCallbackState(URI uri) throws TokenValidationException {
        try {
            ClientResource resource = new ClientResource(uri);
            Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(AMSTSConstants.RESTLET_HEADER_KEY);
            if (headers == null) {
                headers = new Series<Header>(Header.class);
                resource.getRequestAttributes().put(AMSTSConstants.RESTLET_HEADER_KEY, headers);
            }
            headers.set(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            return parseCallbackFromRepresentation(resource.post(null));
        } catch (org.restlet.resource.ResourceException e) {
            throw new TokenValidationException(e.getStatus().getCode(), "Exception caught consuming rest authN to " +
                    "obtain json callback to set X509 Cert: " + e, e);
        }
    }

    private JsonValue parseCallbackFromRepresentation(Representation representation) {
        //TODO: parse the callback structure.
        return null;
    }

    private JsonValue setCertificateInCallback(JsonValue callbackState, X509Certificate certificate) throws TokenValidationException {
        try {
            String base64Cert =  javax.xml.bind.DatatypeConverter.printBase64Binary(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            String message = "Exception caught encoding cert: " + e;
            logger.error(message, e);
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.BAD_REQUEST, message);
        }
        //TODO: set the cert in the parsed callback.
        return null;
    }

    private Representation postFulfilledCallback(JsonValue fulfilledCallback, URI uri) throws TokenValidationException {
        StringRepresentation stringRepresentation =
                new StringRepresentation(fulfilledCallback.toString(), MediaType.APPLICATION_JSON);
        try {
            return new ClientResource(uri).post(stringRepresentation);
        } catch (org.restlet.resource.ResourceException e) {
            throw new TokenValidationException(e.getStatus().getCode(), "Exception caught posting X509 Cert in json callback " +
                    "to rest authN: " + e, e);
        }
    }
}
