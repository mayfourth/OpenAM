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
urn:oasis:names:tc:xacml:3.0:function:dateTime-add-dayTimeDuration
This function SHALL take two arguments, the first SHALL be of data-type
“http://www.w3.org/2001/XMLSchema#dateTime” and the second SHALL be of data-type
“http://www.w3.org/2001/XMLSchema#dayTimeDuration”.
It SHALL return a result of “http://www.w3.org/2001/XMLSchema#dateTime”.
This function SHALL return the value by adding the second argument to the first argument
according to the specification of adding durations to date and time [XS] Appendix E.
*/

import org.forgerock.openam.xacml.v3.Entitlements.FunctionArgument;
import org.forgerock.openam.xacml.v3.Entitlements.XACML3EntitlementException;
import org.forgerock.openam.xacml.v3.Entitlements.XACMLEvalContext;
import org.forgerock.openam.xacml.v3.Entitlements.XACMLFunction;

public class DatetimeAddDaytimeduration extends XACMLFunction {

    public DatetimeAddDaytimeduration()  {
    }
    public FunctionArgument evaluate( XACMLEvalContext pip) throws XACML3EntitlementException {
        return FunctionArgument.falseObject;
    }
}