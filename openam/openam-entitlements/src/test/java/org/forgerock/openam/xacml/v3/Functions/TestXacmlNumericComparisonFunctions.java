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

import org.forgerock.openam.xacml.v3.Entitlements.DataType;
import org.forgerock.openam.xacml.v3.Entitlements.DataValue;
import org.forgerock.openam.xacml.v3.Entitlements.FunctionArgument;
import org.forgerock.openam.xacml.v3.Entitlements.XACML3EntitlementException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A.3.6 Numeric comparison functions
 These functions form a minimal set for comparing two numbers, yielding a Boolean result.
 For doubles they SHALL comply with the rules governed by IEEE 754 [IEEE754].

 urn:oasis:names:tc:xacml:1.0:function:integer-greater-than
 urn:oasis:names:tc:xacml:1.0:function:integer-greater-than-or-equal
 urn:oasis:names:tc:xacml:1.0:function:integer-less-than
 urn:oasis:names:tc:xacml:1.0:function:integer-less-than-or-equal
 urn:oasis:names:tc:xacml:1.0:function:double-greater-than
 urn:oasis:names:tc:xacml:1.0:function:double-greater-than-or-equal
 urn:oasis:names:tc:xacml:1.0:function:double-less-than
 urn:oasis:names:tc:xacml:1.0:function:double-less-than-or-equal

 */

/**
 * XACML Numeric Comparison Functions
 * <p/>
 * Testing Functions as specified by OASIS XACML v3 Core specification.
 *
 * @author Jeff.Schenk@ForgeRock.com
 */
public class TestXacmlNumericComparisonFunctions {

    static final FunctionArgument trueObject = new DataValue(DataType.XACMLBOOLEAN, "true");
    static final FunctionArgument falseObject = new DataValue(DataType.XACMLBOOLEAN, "false");


    @BeforeClass
    public void before() throws Exception {
    }

    @AfterClass
    public void after() throws Exception {
    }

    /**
     *  urn:oasis:names:tc:xacml:1.0:function:integer-greater-than
     */
    @Test
    public void testIntegerGreaterThan() throws XACML3EntitlementException {

    }

    /**
     *  urn:oasis:names:tc:xacml:1.0:function:integer-greater-than-or-equal
     */
    @Test
    public void testIntegerGreaterThanOrEqual() throws XACML3EntitlementException {

    }

    /**
     *  urn:oasis:names:tc:xacml:1.0:function:integer-less-than
     */
    @Test
    public void testIntegerLessThan() throws XACML3EntitlementException {

    }

    /**
     * urn:oasis:names:tc:xacml:1.0:function:integer-less-than-or-equal
     */
    @Test
    public void testIntegerLessThanOrEqual() throws XACML3EntitlementException {

    }

    /**
     *  urn:oasis:names:tc:xacml:1.0:function:integer-greater-than
     */
    @Test
    public void testDoublerGreaterThan() throws XACML3EntitlementException {

    }

    /**
     *  urn:oasis:names:tc:xacml:1.0:function:integer-greater-than-or-equal
     */
    @Test
    public void testDoubleGreaterThanOrEqual() throws XACML3EntitlementException {

    }

    /**
     *  urn:oasis:names:tc:xacml:1.0:function:integer-less-than
     */
    @Test
    public void testDoubleLessThan() throws XACML3EntitlementException {

    }

    /**
     * urn:oasis:names:tc:xacml:1.0:function:integer-less-than-or-equal
     */
    @Test
    public void testDoubleLessThanOrEqual() throws XACML3EntitlementException {

    }


}