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
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class ApplicationTypesResourceTest {

    private ApplicationTypesResource testResource;

    private ApplicationTypeManagerWrapper typeManager;
    private SubjectContext subjectContext;
    private ServerContext serverContext;

    @BeforeMethod
    public void setUp() {
        subjectContext = mock(SubjectContext.class);
        serverContext = new ServerContext(subjectContext);
        typeManager = mock(ApplicationTypeManagerWrapper.class);
        testResource = new ApplicationTypesResource(typeManager, mock(Debug.class));
    }

    @Test
    public void undefinedSubjectShouldFail() {
        ReadRequest request = mock(ReadRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        testResource.readInstance(serverContext, "test", request, handler);

        verifyNoMoreInteractions(request, handler, typeManager, subjectContext, serverContext);
    }

}
