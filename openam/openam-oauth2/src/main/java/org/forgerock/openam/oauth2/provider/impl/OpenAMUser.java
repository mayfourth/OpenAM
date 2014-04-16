/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.openam.oauth2.provider.impl;

import org.forgerock.oauth2.core.ResourceOwner;
import org.restlet.security.User;

import com.iplanet.sso.SSOToken;

/**
 * An OpenAMUser wraps the {@link SSOToken} of the authenticated {@link User}
 *
 */
public class OpenAMUser extends User implements ResourceOwner {
    private SSOToken token;

    public OpenAMUser(String name, SSOToken token) {
        super(name);
        // name = token.getProperty("UserId")
        this.token = token;
    }

    public SSOToken getToken() {
        return token;
    }

    public String getId() {
        return getName();
    }
}
