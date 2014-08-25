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

package org.forgerock.openam.sts.token.validator.wss.disp;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.utils.JsonValueBuilder;

/**
 * @see org.forgerock.openam.sts.token.validator.wss.disp.RestX509CallbackParser
 */
public class RestX509CallbackParserImpl implements RestX509CallbackParser {
    /*
    Based on the rest authN documentation, and the nature of the json structure produced by the RestAuthX509CallbackHandler,
    the structure that this class expects will look like the structure below.
    {
    "authId": "...jwt-value...",
    "template": "",
    "stage": "DataStore1",
    "callbacks": [
        {
            "type": "X509CertificateCallback",
            "output": [
                {
                    "name": "prompt",
                    "value": "certificate"
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": ""
                }
            ]
        }
    ]
}
     */
    public JsonValue updateCallbackWithCertificateState(String initialCallback, String base64EncodedCertificate)
            throws TokenMarshalException {
        JsonValue jsonCallbackStructure = JsonValueBuilder.toJsonValue(initialCallback);
        JsonValue inputMap = jsonCallbackStructure.get(new JsonPointer("/callbacks/0/input/0"));
        if ((inputMap != null) && inputMap.isMap()) {
            inputMap.asMap().put("value", base64EncodedCertificate);
            return jsonCallbackStructure;
        }
        throw new TokenMarshalException(ResourceException.INTERNAL_ERROR, "Could not set x509 cert in json callback. " +
                "Json callback format unexpected. Callback state: " + initialCallback);
    }
}
