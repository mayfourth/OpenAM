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

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.json.JSONObject;

import javax.security.auth.callback.NameCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Defines methods to update a NameCallback from the headers and request of a Rest call and methods to convert a
 * Callback to and from a JSON representation.
 */
public class RestAuthHiddenValueCallbackHandler extends AbstractRestAuthCallbackHandler<HiddenValueCallback> {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final String CALLBACK_NAME = "HiddenValueCallback";

    /**
     * {@inheritDoc}
     */
    public String getCallbackClassName() {
        return CALLBACK_NAME;
    }

    /**
     * Checks the request for the presence of a header "X-OpenAM-Username", if set and not an empty string then
     * sets the header value on the Callback and returns true. Otherwise does nothing and returns false.
     *
     * {@inheritDoc}
     */
    boolean doUpdateCallbackFromRequest(HttpServletRequest request, HttpServletResponse response,
                                        HiddenValueCallback callback) throws RestAuthResponseException {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public HiddenValueCallback handle(HttpServletRequest request, HttpServletResponse response,
                               JsonValue postBody, HiddenValueCallback originalCallback) {
        return originalCallback;
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue convertToJson(HiddenValueCallback callback, int index) {

        String id = callback.getId();
        String value = callback.getValue();

        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("type", CALLBACK_NAME)
                .array("output")
                .addLast(createOutputField("value", value))
                .array("input")
                .addLast(createInputField(index, id))
                .build();

        return jsonValue;
    }

    /**
     * {@inheritDoc}
     */
    public HiddenValueCallback convertFromJson(HiddenValueCallback callback, JsonValue jsonCallback) throws
            RestAuthException {

        validateCallbackType(CALLBACK_NAME, jsonCallback);

        JsonValue input = jsonCallback.get("input");

        if (input.size() != 1) {
            throw new JsonException("JSON Callback does not include an input field");
        }

        JsonValue inputField = input.get(0);
        String value = inputField.get("value").asString();
        callback.setValue(value);

        return callback;
    }
}
