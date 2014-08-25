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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import com.sun.identity.authentication.spi.X509CertificateCallback;
import com.sun.identity.shared.encode.Base64;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Defines methods to update a X509CertificateCallback from the headers and request of a Rest call and methods to
 * convert a Callback to and from a JSON representation.
 */
public class RestAuthX509CallbackHandler extends AbstractRestAuthCallbackHandler<X509CertificateCallback> {

    private static final String CALLBACK_NAME = "X509CertificateCallback";

    /**
     * Checks the request for the presence of a parameter named 'javax.servlet.request.X509Certificate', if present
     * and not null or empty takes the first certificate from the array and sets it on the X509CerificateCallback and
     * returns true. Returns false if this parameter does not reference a certificate.
     *
     * {@inheritDoc}
     */
    public boolean updateCallbackFromRequest(HttpServletRequest request,
            HttpServletResponse response, X509CertificateCallback callback) {

        X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(
                "javax.servlet.request.X509Certificate");

        if (certificates != null && certificates.length > 0) {
            callback.setCertificate(certificates[0]);
            return true;
        }

        return false;
    }

    /**
     * This method will never be called as the <code>updateCallbackFromRequest</code> method from
     * <code>AbstractRestAuthCallbackHandler</code> has been overridden.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response,
            X509CertificateCallback callback) throws RestAuthResponseException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public X509CertificateCallback handle(HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, X509CertificateCallback originalCallback) {
        return originalCallback;
    }

    /**
     * {@inheritDoc}
     */
    public String getCallbackClassName() {
        return CALLBACK_NAME;
    }

    /**
     * Called by the RestAuthCallbackHandlerManager when updateCallbackFromRequest returns false, indicating that
     * certificate state is not present in the javax.servlet.request.X509Certificate parameter. Will provide a json
     * payload which will allow the caller to specify the X509Certificate.
     *
     * {@inheritDoc}
     */
    public JsonValue convertToJson(X509CertificateCallback callback, int index) throws RestAuthException {
        String prompt = callback.getPrompt();

        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME)
                .array("output")
                .addLast(createOutputField("prompt", prompt))
                .array("input")
                .addLast(createInputField(index, ""))
                .build();

        return jsonValue;
    }

    /**
     * {@inheritDoc}
     */
    public X509CertificateCallback convertFromJson(X509CertificateCallback callback, JsonValue jsonCallback) throws RestAuthException {
        validateCallbackType(CALLBACK_NAME, jsonCallback);

        JsonValue input = jsonCallback.get("input");
        if (!input.isList() || input.size() != 1) {
            throw new JsonException("X509CertificateCallback does not include a input field");
        }

        JsonValue inputField = input.get(0);
        String certString = inputField.get("value").asString();
        try {
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                    new ByteArrayInputStream(Base64.decode(certString.getBytes("UTF-8"))));
            callback.setCertificate(certificate);
            return callback;
        } catch (CertificateException e) {
            throw new RestAuthException(ResourceException.INTERNAL_ERROR,
                    "Exception caught marshalling X509 cert from value set in json callback: " + e, e);
        } catch (UnsupportedEncodingException e) {
            throw new RestAuthException(ResourceException.INTERNAL_ERROR,
                    "Exception caught marshalling X509 cert from value set in json callback: " + e, e);
        }
    }
}
