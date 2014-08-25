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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.service.invocation;

import com.sun.identity.shared.encode.Base64;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This class is a representation of a X509Certificate used as an input-token-type in an invocation to the rest-sts.
 * It is similar to the ProofTokenState, insofar as both encapsulate X509Certificates, but the ProofTokenState is
 * used specifically for SAML2 SubjectConfirmation and the proof keys defined in the ws-trust specification. See the
 * ProofTokenState class for details. This class serves to encapsulate a X509Certificate and a X509 token_type to indicate
 * to the rest-sts that a token transformation with an X509Certificate as input is being invoked.
 */
public class X509TokenState {
    public static class X509TokenStateBuilder {
        private X509Certificate certificate;

        public X509TokenStateBuilder x509Certificate(X509Certificate certificate) {
            this.certificate = certificate;
            return this;
        }

        public X509TokenState build() throws TokenMarshalException {
            return new X509TokenState(this);
        }
    }
    private static final String BASE_64_ENCODED_CERTIFICATE = "base64EncodedCertificate";
    private static final String X_509 = "X.509";

    private final X509Certificate certificate;

    private X509TokenState(X509TokenStateBuilder builder) throws TokenMarshalException {
        certificate = builder.certificate;
        if (certificate == null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "the X509Certificate state must be set.");
        }
    }

    public X509Certificate getX509Certificate() {
        return certificate;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof X509TokenState) {
            X509TokenState otherState = (X509TokenState)other;
            return (certificate.equals(otherState.certificate));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return certificate.hashCode();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static X509TokenStateBuilder builder() {
        return new X509TokenStateBuilder();
    }

    public static X509TokenState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        final String certString = jsonValue.get(BASE_64_ENCODED_CERTIFICATE).asString();
        try {
            final X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance(X_509).generateCertificate(
                    new ByteArrayInputStream(Base64.decode(certString.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))));
            return X509TokenState.builder().x509Certificate(x509Certificate).build();
        } catch (CertificateException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                    "Exception caught marshalling from json to X509 cert: " + e, e);
        } catch (UnsupportedEncodingException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                    "Exception caught marshalling from json to X509 cert: " + e, e);
        }
    }

    public JsonValue toJson() throws IllegalStateException {
        try {
            String base64EncodedCertificate = Base64.encode(certificate.getEncoded());
            return json(object(field(BASE_64_ENCODED_CERTIFICATE, base64EncodedCertificate),
                    field(AMSTSConstants.TOKEN_TYPE_KEY, TokenType.X509.name())));
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Exception getting base64 representation of certificate: " + e, e);
        }
    }

}
