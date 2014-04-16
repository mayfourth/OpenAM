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

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationTypeManagerWrapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

/**
 * @since 12.0.0
 */
public class ApplicationsResourceTest {

    private ApplicationsResource applicationsResource;

    private Debug debug;
    private ApplicationManagerWrapper applicationManagerWrapper;
    private ApplicationTypeManagerWrapper applicationTypeManagerWrapper;

    @BeforeMethod
    public void setUp() {

        debug = mock(Debug.class);
        applicationManagerWrapper = mock(ApplicationManagerWrapper.class);
        applicationTypeManagerWrapper = mock(ApplicationTypeManagerWrapper.class);

        applicationsResource = new ApplicationsResource(debug, applicationManagerWrapper,
                applicationTypeManagerWrapper);
    }



    // Davids Tests


    // Roberts Tests
    @Test
    public void shouldReturnNullIfSubjectNullOnRead() {
        fail();
    }

    @Test
    public void shouldThrowInternalErrorIfResourceCouldNotBeRetrievedOnRead() {
        fail();
    }

    @Test
    public void shouldThrowInternalErrorIfJSONFormatFailsOnRead() {
        fail();
    }

    @Test
    public void shouldUseResourceIDForFetchingApplicationOnRead() {
        fail();
    }

    @Test
    public void shouldUseRealmFromContextSubjectOnRead() {
        fail();
    }

    @Test
    public void shouldUseSubjectFromContextOnRead() {
        fail();
    }

    // Phills Tests

}
