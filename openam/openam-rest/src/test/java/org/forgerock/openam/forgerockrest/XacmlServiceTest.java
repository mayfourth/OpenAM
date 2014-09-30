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

package org.forgerock.openam.forgerockrest;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.xacml3.XACMLImportExport;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.ResourceException;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class XacmlServiceTest {

    private static final ConcurrentMap<String, Object> REQUEST_ATTRIBUTES = new ConcurrentHashMap<String, Object>() {{
        put("realm", "/");
    }};

    private AdminTokenAction adminTokenAction;
    private XacmlService service;
    private XACMLImportExport importExport;
    private Debug debug;
    private Response response;
    private Request request;

    private Answer<SSOToken> ssoTokenAnswer = new Answer<SSOToken>() {
        @Override
        public SSOToken answer(InvocationOnMock invocation) throws Throwable {
            SSOToken ssoToken = mock(SSOToken.class);
            doReturn("my-uuid").when(ssoToken).getProperty(Constants.UNIVERSAL_IDENTIFIER);
            return ssoToken;
        }
    };

    @BeforeMethod
    public void setup() throws Exception {
        this.importExport = mock(XACMLImportExport.class);
        this.debug = mock(Debug.class);
        this.adminTokenAction = mock(AdminTokenAction.class);
        doAnswer(ssoTokenAnswer).when(adminTokenAction).run();
        this.service = new XacmlService(importExport, adminTokenAction, debug);
        this.request = mock(Request.class);
        doReturn(REQUEST_ATTRIBUTES).when(request).getAttributes();
        this.response = mock(Response.class);
        service.setRequest(request);
        service.setResponse(response);
    }

    @Test
    public void testImportXACML() throws Exception {
        //given
        Representation representation = mock(Representation.class);
        InputStream is = new ByteArrayInputStream("Hello World".getBytes());
        doReturn(is).when(representation).getStream();
        doReturn(true).when(importExport).importXacml(eq("/"), eq(is), any(Subject.class));

        //when
        Representation result = service.importXACML(representation);

        //then
        assertThat(result).isInstanceOf(EmptyRepresentation.class);
        verify(response).setStatus(Status.SUCCESS_NO_CONTENT);
    }

    @Test
    public void testImportXACMLIOException() throws Exception {
        //given
        Representation representation = mock(Representation.class);
        doThrow(new IOException()).when(representation).getStream();

        try {
            //when
            service.importXACML(representation);

            //then
            fail("Expect exception");
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
        }
    }

    @Test
    public void testImportXACMLNoPolicies() throws Exception {
        //given
        Representation representation = mock(Representation.class);
        InputStream is = new ByteArrayInputStream("Hello World".getBytes());
        doReturn(is).when(representation).getStream();
        doReturn(false).when(importExport).importXacml(eq("/"), eq(is), any(Subject.class));

        try {
            //when
            service.importXACML(representation);

            //then
            fail("Expect exception");
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(ResourceException.BAD_REQUEST);
            assertThat(e.getMessage()).isEqualTo("No policies found in XACML document");
        }
    }

    @Test
    public void testImportXACMLImportFailure() throws Exception {
        //given
        Representation representation = mock(Representation.class);
        InputStream is = new ByteArrayInputStream("Hello World".getBytes());
        doReturn(is).when(representation).getStream();
        EntitlementException failure = new EntitlementException(EntitlementException.JSON_PARSE_ERROR);
        doThrow(failure).when(importExport).importXacml(eq("/"), eq(is), any(Subject.class));

        try {
            //when
            service.importXACML(representation);

            //then
            fail("Expect exception");
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(ResourceException.BAD_REQUEST);
            assertThat(e.getMessage()).isEqualTo("JSON Exception.");
        }
    }

    @Test
    public void testExportXACML() throws Exception {
        //given
        Form query = new Form();
        query.add(XacmlService.QUERY_PARAM_STRING, "test1");
        query.add(XacmlService.QUERY_PARAM_STRING, "test2");
        service = spy(service);
        doReturn(query).when(service).getQuery();
        PolicySet policySet = new PolicySet();
        doReturn(policySet).when(importExport).exportXACML(eq("/"), any(Subject.class), any(List.class));

        //when
        Representation result = service.exportXACML();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.write(baos);
        String xml = new String(baos.toByteArray(), "UTF-8");

        //then
        assertThat(xml).contains("<PolicySet");
        assertThat(xml).contains("xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\"");
        verify(response).setStatus(Status.SUCCESS_OK);
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(importExport).exportXACML(eq("/"), any(Subject.class), listCaptor.capture());
        assertThat(listCaptor.getValue()).containsExactly("test1", "test2");
    }

    @Test
    public void testExportXACMLEntitlementException() throws Exception {
        //given
        Form query = new Form();
        service = spy(service);
        doReturn(query).when(service).getQuery();
        EntitlementException ee = new EntitlementException(EntitlementException.JSON_PARSE_ERROR);
        doThrow(ee).when(importExport).exportXACML(eq("/"), any(Subject.class), any(List.class));

        try {
            //when
            Representation result = service.exportXACML();

            //then
            fail("Expect exception");
        } catch (ResourceException e) {
            assertThat(e.getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
            assertThat(e.getMessage()).isEqualTo("JSON Exception.");
        }
    }
}