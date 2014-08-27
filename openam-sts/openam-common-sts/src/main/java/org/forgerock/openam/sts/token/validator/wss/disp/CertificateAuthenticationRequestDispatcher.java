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

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import com.google.inject.Inject;
import com.sun.identity.shared.encode.Base64;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;
import org.restlet.data.MediaType;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

import java.net.URI;

import org.restlet.util.Series;
import org.slf4j.Logger;

/**
 * Class which encapsulates knowledge as to how to post a x509 certificate to the OpenAM REST authN context. This class
 * will initiate the authN process to receive the json callback with a placeholder for an X509Certificate, and set this
 * reference, and return the json callback state.
 */
public class CertificateAuthenticationRequestDispatcher implements TokenAuthenticationRequestDispatcher<X509Certificate[]> {
    private final RestX509CallbackParser callbackParser;
    private final Logger logger;

    @Inject
    public CertificateAuthenticationRequestDispatcher(RestX509CallbackParser callbackParser, Logger logger) {
        this.callbackParser = callbackParser;
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
        return postCertInHeader(uri, certificates[0], target);

//        final JsonValue fulfilledCallback = initiateCertAuthentication(uri, certificates[0]);
//        return postFulfilledCallback(fulfilledCallback, uri);
    }

    private Representation postCertInHeader(URI uri, X509Certificate certificate,
                                            AuthTargetMapping.AuthTarget target) throws TokenValidationException {
        final String base64Certificate;
        try {
            base64Certificate = Base64.encode(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.BAD_REQUEST,
                    "Could not obtain the base64-encoded representation of the client certificate: " + e, e);
        }
        ClientResource resource = new ClientResource(uri);
        Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(AMSTSConstants.RESTLET_HEADER_KEY);
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put(AMSTSConstants.RESTLET_HEADER_KEY, headers);
        }
        headers.set(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        if (target == null) {
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.BAD_REQUEST,
                    "When validatating X509 Certificates, an AuthTarget needs to be configured with a Map containing a String " +
                            "entry referenced by key" + AMSTSConstants.X509_TOKEN_AUTH_TARGET_HEADER_KEY +
                            " which specifies the header name which will reference the OIDC ID Token.");
        }
        Object headerKey = target.getContext().get(AMSTSConstants.X509_TOKEN_AUTH_TARGET_HEADER_KEY);
        if (!(headerKey instanceof String)) { //checks both for null and String
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.BAD_REQUEST,
                    "When validatating X509 Certificates, an AuthTarget needs to be configured with a Map containing a String " +
                            "entry referenced by key" + AMSTSConstants.X509_TOKEN_AUTH_TARGET_HEADER_KEY +
                            " which specifies the header name which will reference the OIDC ID Token.");
        }
        headers.set((String)headerKey, base64Certificate);
        try {
            return resource.post(null);
        } catch (org.restlet.resource.ResourceException e) {
            throw new TokenValidationException(e.getStatus().getCode(), "Exception caught posting X509 Certificate " +
                    "to rest authN: " + e, e);
        }
    }

    private JsonValue initiateCertAuthentication(URI uri, X509Certificate certificate) throws TokenValidationException {
        try {
            ClientResource resource = new ClientResource(uri);
            Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(AMSTSConstants.RESTLET_HEADER_KEY);
            if (headers == null) {
                headers = new Series<Header>(Header.class);
                resource.getRequestAttributes().put(AMSTSConstants.RESTLET_HEADER_KEY, headers);
            }
            headers.set(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            final Representation representation = resource.post(null);
            final String base64Certificate = Base64.encode(certificate.getEncoded());
            return callbackParser.updateCallbackWithCertificateState(representation.getText(), base64Certificate);
        } catch (org.restlet.resource.ResourceException e) {
            //thrown by resource.post(null)
            throw new TokenValidationException(e.getStatus().getCode(), "Exception caught consuming rest authN to " +
                    "obtain json callback to set X509 Cert: " + e, e);
        } catch (IOException e) {
            //thrown by representation.getText()
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, "Exception caught consuming rest authN to " +
                    "obtain json callback to set X509 Cert: " + e, e);

        } catch (CertificateEncodingException e) {
            //thrown by certificate.getEncoded.
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, "Exception caught obtaining encoded " +
                    "certificate state prior to updating json callback with certificate state: " + e, e);
        } catch (TokenMarshalException e) {
            //thrown by callbackParser.updateCallbackWithCertificateState
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, "Exception caught updating json callback " +
                    "with certificate state: " + e, e);
        }
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
