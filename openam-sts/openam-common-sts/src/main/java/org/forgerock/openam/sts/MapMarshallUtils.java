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

package org.forgerock.openam.sts;

import org.forgerock.openam.utils.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class encapsulates a set of public static methods to aid in marshalling Rest and Soap STS instance config state,
 * ultimately a json native format, into the Map<String, Set<String>> of attributes required for SMS persistence. As part
 * of this conversion, a Map<String, Object>, returned from JsonValue.asMap, must be transformed into a Map<String, Set<String>>.
 * Likewise, when going in the other direction (SMS map to JsonValue), the Map<String, Set<String>> needs to be converted
 * back to a Map<String,Object> so that a JsonValue can be constructed therefrom.
 *
 * Static methods cannot be mocked (without bcel), and thus should be kept to a minimum. The preferred guice interface/implementation
 * method was not adopted because this method is consumed from the config/user classes, like RestSTSInstanceConfig, which
 * do not have dependencies injected, as they define a client SDK, which facilitates the programmatic publishing of
 * STS instances.
 */
public class MapMarshallUtils {
    /**
     * Marshals from a Map<String,Object> produced by jsonValue.asMap to the Map<String, Set<String>> expected by the
     * SMS.
     * @param jsonMap
     * @return the Map<String, Set<String>> attributes expected by the SMS
     */
    public static Map<String, Set<String>> toSmsMap(Map<String, Object> jsonMap) {
        Map<String, Set<String>> smsMap = new HashMap<String, Set<String>>(jsonMap.size());
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            if (entry.getValue() != null) {
                smsMap.put(entry.getKey(), CollectionUtils.asSet(entry.getValue().toString()));
            } else {
                smsMap.put(entry.getKey(), Collections.EMPTY_SET);
            }
        }
        return smsMap;
    }

    /**
     * Note that this method will only return the first element in the Set<String>. Its intent is only to aid in the
     * conversion between String and Set<String> for non-complex values. I would like to exclude Set instances with
     * a cardinality != 1, but encapsulated complex types will be passed the Map<String,Set<String>> corresponding to the
     * top-level object when the object hierarchy is re-constituted from SMS state, which means that some Map entries
     * will have Set<String> instances with a cardinality != 1. Yet this should not affect the leaf objects, as they do
     * not encapsulate any other complex objects.
     * @param attributeMap The map of STS instance attributes obtained from the SMS
     * @return the Map<String, Object> format which can be used to construct a JsonValue
     */
    public static Map<String, Object> toJsonValueMap(Map<String, Set<String>> attributeMap) {
        Map<String, Object> jsonValueMap = new HashMap<String, Object>(attributeMap.size());
        for (Map.Entry<String, Set<String>> entry : attributeMap.entrySet()) {
            if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                jsonValueMap.put(entry.getKey(), entry.getValue().iterator().next());
            } else {
                jsonValueMap.put(entry.getKey(), null);
            }
        }
        return jsonValueMap;
    }
}
