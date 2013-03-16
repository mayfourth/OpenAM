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
package org.forgerock.openam.xacml.v3.Functions;

/*
urn:oasis:names:tc:xacml:1.0:function:string-normalize-space
This function SHALL take one argument of data-type “http://www.w3.org/2001/XMLSchema#string”
and SHALL normalize the value by stripping off all leading and trailing white space characters.
The whitespace characters are defined in the metasymbol S (Production 3) of [XML].
*/

import org.forgerock.openam.xacml.v3.Entitlements.*;

/**
 * urn:oasis:names:tc:xacml:1.0:function:string-normalize-space
 */
public class StringNormalizeSpace extends XACMLFunction {

    public StringNormalizeSpace()  {
    }
    public FunctionArgument evaluate( XACMLEvalContext pip) throws XACML3EntitlementException {
        if (getArgCount() != 1) {
            throw new XACML3EntitlementException("Function Requires 1 argument, " +
                    "however " + getArgCount() + " in stack.");
        }
        FunctionArgument retVal = new DataValue(DataType.XACMLSTRING,getArg(0).asString(pip).trim());
        // return the Value.
        return retVal;
    }
}