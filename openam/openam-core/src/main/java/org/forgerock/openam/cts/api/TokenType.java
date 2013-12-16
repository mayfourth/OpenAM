/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.cts.api;

/**
 * Responsible for defining the available token types in the Core Token Service.
 *
 * Note: If a new token type is added in the future, this enum will need to be updated.
 *
 * @author Robert Wapshott
 */
public enum TokenType {
    SESSION(),
    SAML2(),
    OAUTH(),
    REST();

    /**
     * Retrieves the appropriate TokenType from the list of avaliable
     * enums that matches on the ordinal index.
     *
     * @param ordinalIndex the ordinal index to look up
     * @return the TokenType this ordinal value represents, null otherwise
     */
    public static TokenType getTokenFromOrdinalIndex(int ordinalIndex) {

        if(ordinalIndex < 0 || ordinalIndex > TokenType.values().length) {
            return null;
        }

        return TokenType.values()[ordinalIndex];
    }

}
