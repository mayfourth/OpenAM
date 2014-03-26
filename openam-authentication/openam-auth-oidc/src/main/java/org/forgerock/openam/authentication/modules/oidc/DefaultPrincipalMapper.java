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

import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.authentication.modules.oidc.PrincipalMapper
 */
public class DefaultPrincipalMapper implements PrincipalMapper {
    public Map<String, Set<String>> getAttributesForPrincipalLookup(Map<String, String> localToJwtAttributeMapping,
                                                                    SignedJwt jwt,
                                                                    JwtClaimsSet jwtClaimsSet,
                                                                    URL profileServiceUrl) {
        Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();
        for (Map.Entry<String, String> entry : localToJwtAttributeMapping.entrySet()) {
            if (jwtClaimsSet.isDefined(entry.getValue())) {
                Set<String> value = new HashSet<String>();
                value.add(entry.getValue().toString());
                attributes.put(entry.getKey(), value);
            }
        }
        return attributes;
    }
}
