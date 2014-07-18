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
package org.forgerock.openam.authentication.modules.scripted;

public class ScriptedClientUtilityFunctions {

    // Create an anonymous function and pass it the name of the hidden output callback/element:
    public static String createClientSideScriptExecutorFunction(String script, String outputParameterName) {
        String clientSideScriptFunction = "(function(output){\n" +
                script +
                "\n})" +
                "(document.forms[0].elements['" + outputParameterName + "']);\n";
        return clientSideScriptFunction;
    }

    // Auto submission logic for the form:
    public static String createAutoSubmissionLogic() {
        String autoSubmit = "" +
                "if(!(window.jQuery)) {\n" + // Crude detection to see if XUI is not present.
                    "document.forms[0].submit();\n" +
                "} else {\n" +
                    "$('input[type=submit]').trigger('click');\n" +
                "}";

        return autoSubmit;
    }

}
