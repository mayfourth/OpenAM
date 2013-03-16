/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.xacml.v3.Entitlements;


import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.xacml3.core.Policy;

/*
*  This class is here to we can instantiate it from OpenAM without messing with Linking issues
*
*  It should return a new privilege object,  populated with the XACML3 policy
*/
public class XACML3Extension implements XACML3Interface {

    public Privilege XACML3NewPolicy(Policy pol) {

        XACML3Policy xPol = new XACML3Policy(pol);
         return null;

    };


}