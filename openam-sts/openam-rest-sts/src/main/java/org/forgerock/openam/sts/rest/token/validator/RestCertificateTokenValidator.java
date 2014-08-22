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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.token.validator;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.X509Security;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Text;

import java.security.Principal;
import java.security.cert.X509Certificate;

public class RestCertificateTokenValidator implements TokenValidator {
    public static final String X509_V3_TYPE = WSConstants.X509TOKEN_NS + "#X509v3";
    public static final String BASE64_ENCODING_TYPE = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";
    private final AuthenticationHandler<X509Certificate[]> authenticationHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;


    /*
    ctor not injected as it is constructed by the TokenTransformFactoryImpl
     */
    public RestCertificateTokenValidator(AuthenticationHandler<X509Certificate[]> authenticationHandler,
                                         ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                         PrincipalFromSession principalFromSession) {
        this.authenticationHandler = authenticationHandler;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;

    }
    @Override
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        Object token = validateTarget.getToken();
        return (token instanceof BinarySecurityTokenType)
                && X509_V3_TYPE.equals(((BinarySecurityTokenType)token).getValueType());
    }

    @Override
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(ReceivedToken.STATE.INVALID);
        response.setToken(validateTarget);

        X509Certificate[] x509Certificates;
        if (validateTarget.isBinarySecurityToken()) {
            x509Certificates = marshalBinarySecurityTokenToCertArray((BinarySecurityTokenType)validateTarget.getToken());
        } else {
            //No toString in ReceivedToken, so I can't log what I really have
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "Token passed to RestCertificateTokenValidator not BinarySecurityToken, as expected.");
        }
        try {
            authenticationHandler.authenticate(makeRequestData(tokenParameters), x509Certificates);
            /*
            a successful call to the authenticationHandler will put the sessionId in the tokenCache. Pull it
            out and use it to obtain the principal corresponding to the Session.
             */
            Principal principal = principalFromSession.getPrincipalFromSession(threadLocalAMTokenCache.getAMToken());
            response.setPrincipal(principal);
            validateTarget.setState(ReceivedToken.STATE.VALID);
        } catch (TokenValidationException e) {
            throw new AMSTSRuntimeException(ResourceException.FORBIDDEN,
                    "Exception caught validating OIDC token with authentication handler: " + e, e);
        } catch (TokenCreationException e) {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "No OpenAM Session token cached: " + e, e);
        }
        return response;
    }

    /*
     * Creates the RequestData object in a manner similar to the org.apache.cxf.sts.token.validator.UsernameTokenValidator.
     * As the name implies, the RequestData provides request context. For the AuthenticationHandler<OpenIDConnectToken>
     * encapsulated in this class, the entity which will dispatch the request to the OpenAM Rest authN context, this
     * data is not used, though it is provided simply to fulfill the contract.
     */
    private RequestData makeRequestData(TokenValidatorParameters parameters) {
        STSPropertiesMBean stsProperties = parameters.getStsProperties();
        RequestData requestData = new RequestData();
        requestData.setSigCrypto(stsProperties.getSignatureCrypto());
        requestData.setCallbackHandler(stsProperties.getCallbackHandler());
        return requestData;
    }

    /*
    TODO: does this really have to be this complicated? from X509Certificate to RecievedToken in the TokenRequestMarshallerImpl,
    back to an X509Certificate - are there possibilities to simplify?
     */
    private X509Certificate[] marshalBinarySecurityTokenToCertArray(BinarySecurityTokenType binarySecurityType) {
        String encodingType = binarySecurityType.getEncodingType();
        if (!BASE64_ENCODING_TYPE.equals(encodingType)) {
            //TODO - throw exception
        }

        Document doc = DOMUtils.createDocument();
        BinarySecurity binarySecurity = new X509Security(doc);
        binarySecurity.setEncodingType(encodingType);
        binarySecurity.setValueType(binarySecurityType.getValueType());
        String data = binarySecurityType.getValue();
        ((Text) binarySecurity.getElement().getFirstChild()).setData(data);

        /*
        try {
            Credential credential = new Credential();
            credential.setBinarySecurityToken(binarySecurity);
            if (sigCrypto != null) {
                X509Certificate cert = ((X509Security)binarySecurity).getX509Certificate(sigCrypto);
                credential.setCertificates(new X509Certificate[]{cert});
            }

        }
        */
        return null;
    }
}
