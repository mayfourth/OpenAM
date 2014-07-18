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
package com.sun.identity.authentication.callbacks;

import org.forgerock.util.Reject;

public class HiddenValueCallback implements javax.security.auth.callback.Callback, java.io.Serializable {

    private String value;
    private final String id;
    private final String defaultValue = "";

    public HiddenValueCallback(String id) {
        Reject.ifNull(id, "A HiddenValueCallback must be given an id.");
        this.id = id;
    }

    public HiddenValueCallback(String id, String value) {
        Reject.ifNull(id, "A HiddenValueCallback must be given an id.");
        this.id = id;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public java.lang.String getDefaultValue() { return defaultValue; }
}
