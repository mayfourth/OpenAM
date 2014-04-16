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

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationWrapper;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;

/**
 * @since 12.0.0
 */
public class ApplicationsResourceTest {

    private ApplicationsResource applicationsResource;

    private Debug debug;
    private ApplicationManagerWrapper applicationManagerWrapper;
    private ApplicationTypeManagerWrapper applicationTypeManagerWrapper;
    private ResultHandler<Resource> mockResultHandler;
    private ApplicationWrapper applicationWrapper;

    @BeforeMethod
    public void setUp() {

        debug = mock(Debug.class);
        applicationManagerWrapper = mock(ApplicationManagerWrapper.class);
        applicationTypeManagerWrapper = mock(ApplicationTypeManagerWrapper.class);

        applicationWrapper = mock(ApplicationWrapper.class);
        applicationsResource = new ApplicationsResource(debug, applicationManagerWrapper,
                applicationTypeManagerWrapper) {
            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject, String realm) throws IOException, EntitlementException {
                return applicationWrapper;
            }
        };

        mockResultHandler = mock(ResultHandler.class);
    }



    // Davids Tests


    // Roberts Tests
    @Test
    public void shouldThrowInternalErrorIfSubjectNotFoundOnRead() {
        // Given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        ServerContext context = new ServerContext(mockSubjectContext);
        given(mockSubjectContext.getCallerSubject()).willReturn(null);

        // When
        applicationsResource.readInstance(context, null, null, mockResultHandler);

        // Then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldUseResourceIDForFetchingApplicationOnRead() throws EntitlementException {
        // Given
        String resourceID = "ferret";

        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "badger");
        ServerContext serverContext = new ServerContext(realmContext);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationManagerWrapper.getApplication(any(Subject.class), anyString(), anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null, mockResultHandler);

        // Then
        verify(applicationManagerWrapper).getApplication(any(Subject.class), anyString(), eq(resourceID));
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
    @Test
    public void shouldDeleteInstance() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        applicationsResource.deleteInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper).deleteApplication(subject, "REALM", resourceId);
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        Resource resource = resourceCaptor.getValue();
        assertEquals(resource.getId(), resourceId);
        assertEquals(resource.getRevision(), "0");
        assertEquals(resource.getContent(), json(object()));
    }

    @Test
    public void shouldNotDeleteInstanceWhenSubjectIsNull() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        ServerContext context = new ServerContext(subjectContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = null;

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        applicationsResource.deleteInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper, never()).deleteApplication(subject, "REALM", resourceId);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void deleteInstanceShouldHandleFailedDeleteApplication() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);
        doThrow(EntitlementException.class).when(applicationManagerWrapper)
                .deleteApplication(subject, "REALM", resourceId);

        //When
        applicationsResource.deleteInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper).deleteApplication(subject, "REALM", resourceId);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void shouldUpdateInstance() throws EntitlementException, IOException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        Application newApplication = mock(Application.class);
        JsonValue response = mock(JsonValue.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "REALM", resourceId)).willReturn(application);
        given(applicationWrapper.getName()).willReturn("APP_NAME");
        given(applicationWrapper.getApplication()).willReturn(newApplication);
        given(newApplication.getLastModifiedDate()).willReturn(1000L);
        given(applicationWrapper.toJsonValue()).willReturn(response);

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper)
                .updateApplication(application, applicationWrapper.getApplication(), subject, "REALM");
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        Resource resource = resourceCaptor.getValue();
        assertEquals(resource.getId(), "APP_NAME");
        assertEquals(resource.getRevision(), "1000");
        assertEquals(resource.getContent(), response);
    }

    @Test
    public void updateInstanceShouldReturnServerInternalExceptionWhenApplicationToJson() throws EntitlementException,
            IOException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        Application newApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "REALM", resourceId)).willReturn(application);
        given(applicationWrapper.getName()).willReturn("APP_NAME");
        given(applicationWrapper.getApplication()).willReturn(newApplication);
        given(newApplication.getLastModifiedDate()).willReturn(1000L);
        doThrow(IOException.class).when(applicationWrapper).toJsonValue();

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper)
                .updateApplication(application, applicationWrapper.getApplication(), subject, "REALM");
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void updateInstanceShouldReturnInternalServerErrorWhenUpdatingFails() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "REALM", resourceId)).willReturn(application);
        doThrow(EntitlementException.class).when(applicationManagerWrapper)
                .updateApplication(application, applicationWrapper.getApplication(), subject, "REALM");

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        verifyZeroInteractions(applicationManagerWrapper);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void shouldNotUpdateInstanceIfApplicationNotFound() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = null;

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "REALM", resourceId)).willReturn(application);

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 404);
        assertEquals(exception.getReason(), "Not Found");
    }

    @Test
    public void shouldNotUpdateInstanceWhenSubjectIsNull() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = null;

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        verifyZeroInteractions(applicationManagerWrapper);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }
}
