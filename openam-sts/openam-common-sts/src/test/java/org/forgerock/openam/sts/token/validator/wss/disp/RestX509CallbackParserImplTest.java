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
import org.forgerock.openam.sts.TokenMarshalException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class RestX509CallbackParserImplTest {
    private static final String INITIAL_CALLBACK_STATE = "{\n" +
            "    \"authId\": \"...jwt-value...\",\n" +
            "    \"template\": \"\",\n" +
            "    \"stage\": \"DataStore1\",\n" +
            "    \"callbacks\": [\n" +
            "        {\n" +
            "            \"type\": \"X509CertificateCallback\",\n" +
            "            \"output\": [\n" +
            "                {\n" +
            "                    \"name\": \"prompt\",\n" +
            "                    \"value\": \"certificate\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"input\": [\n" +
            "                {\n" +
            "                    \"name\": \"IDToken1\",\n" +
            "                    \"value\": \"\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}\n";

    //just for comparison - not even base64 encoded.
    private static final String FAUX_BASE_64_ENCODED_CERT = "faux_cert";

    @Test
    public void testCallbackParsing() throws TokenMarshalException {
        JsonValue updatedCallback =
                new RestX509CallbackParserImpl().updateCallbackWithCertificateState(INITIAL_CALLBACK_STATE,
                        FAUX_BASE_64_ENCODED_CERT);
        assertEquals(FAUX_BASE_64_ENCODED_CERT,
                updatedCallback.get(new JsonPointer("callbacks/0/input/0/value")).asString());
    }
}
