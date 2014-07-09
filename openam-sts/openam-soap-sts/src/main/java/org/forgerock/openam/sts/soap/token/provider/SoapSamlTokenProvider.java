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

package org.forgerock.openam.sts.soap.token.provider;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.request.ReceivedKey;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.token.UsernameToken;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.ws.handler.MessageContext;
import java.security.cert.X509Certificate;


public class SoapSamlTokenProvider implements TokenProvider {
    /*
    The WSS SAML token profile spec does not mandate any content for Bearer SAML assertions. The SAML Web SSO Profile specifies
    that SAML Bearer assertions must have the url of the SP's assertion consumer service, but because there is no standard
    way to include this information in a RST, this state will be passed as null to the TokenGenerationService.
     */
    private static final String NULL_SP_ACS_URL = null;
    private final TokenGenerationServiceConsumer tokenGenerationServiceConsumer;
    private final AMSessionInvalidator amSessionInvalidator;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final String stsInstanceId;
    private final String realm;
    private final XMLUtilities xmlUtilities;
    private final XmlTokenAuthnContextMapper authnContextMapper;
    private final XmlMarshaller<OpenAMSessionToken> amSessionTokenXmlMarshaller;
    private final Logger logger;

    /*
    ctor not injected as this class created by TokenOperationFactoryImpl
     */
    public SoapSamlTokenProvider(TokenGenerationServiceConsumer tokenGenerationServiceConsumer,
                                 AMSessionInvalidator amSessionInvalidator,
                                 ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                 String stsInstanceId,
                                 String realm,
                                 XMLUtilities xmlUtilities,
                                 XmlTokenAuthnContextMapper authnContextMapper,
                                 XmlMarshaller<OpenAMSessionToken> amSessionTokenXmlMarshaller,
                                 Logger logger) {
        this.tokenGenerationServiceConsumer = tokenGenerationServiceConsumer;
        this.amSessionInvalidator = amSessionInvalidator;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.stsInstanceId = stsInstanceId;
        this.realm = realm;
        this.xmlUtilities = xmlUtilities;
        this.authnContextMapper = authnContextMapper;
        this.amSessionTokenXmlMarshaller = amSessionTokenXmlMarshaller;
        this.logger = logger;
    }


    public boolean canHandleToken(String tokenType) {
        return canHandleToken(tokenType, null);
    }

    /*
    Note that I got rid of realm support as defined by the CXF-STS. See
    org.apache.cxf.sts.token.provider.SAMLTokenProvider#canHandleToken for details on the realm support.
     */
    public boolean canHandleToken(String tokenType, String realm) {
        return (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType) || WSConstants.SAML2_NS.equals(tokenType));
    }

    public TokenProviderResponse createToken(TokenProviderParameters tokenProviderParameters) {
        try {
            final TokenProviderResponse tokenProviderResponse = new TokenProviderResponse();
            final SAML2SubjectConfirmation subjectConfirmation = determineSubjectConfirmation(tokenProviderParameters);
            final String authNContextClassRef = getAuthnContextClassRef(tokenProviderParameters);
            String assertion;
            try {
                assertion = getAssertion(tokenProviderParameters, authNContextClassRef, subjectConfirmation);
            } catch (TokenCreationException e) {
                throw new AMSTSRuntimeException(e.getCode(), e.getMessage(), e);
            }
            Document assertionDocument = xmlUtilities.stringToDocumentConversion(assertion);
            if (assertionDocument ==  null) {
                logger.error("Could not turn assertion string returned from TokenGenerationService into DOM Document. " +
                        "The assertion string: " + assertion);
                throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                        "Could not turn assertion string returned from TokenGenerationService into DOM Document.");
            }
            final Element assertionElement = assertionDocument.getDocumentElement();
            tokenProviderResponse.setToken(assertionElement);
            tokenProviderResponse.setTokenId(assertionElement.getAttributeNS(null, "ID"));
            return tokenProviderResponse;
        } finally {
            if (amSessionInvalidator != null) {
                try {
                    amSessionInvalidator.invalidateAMSession(threadLocalAMTokenCache.getAMToken());
                } catch (Exception e) {
                    String message = "Exception caught invalidating interim AMSession: " + e;
                    logger.warn(message, e);
                /*
                The fact that the interim OpenAM session was not invalidated should not prevent a token from being issued, so
                I will not throw a AMSTSRuntimeException
                 */
                }
            }
        }
    }

    /*
    The TokenGenerationService needs to the SAML2SubjectConfirmation as a parameter. This method will return the appropriate
    SubjectConfirmation value, depending upon the KeyType specified in the RST invocation, and also the OnBehalfOf value.
     */
    private SAML2SubjectConfirmation determineSubjectConfirmation(TokenProviderParameters tokenProviderParameters)  {
        String keyType = tokenProviderParameters.getKeyRequirements().getKeyType();
        if (STSConstants.BEARER_KEY_KEYTYPE.equals(keyType)) {
            return SAML2SubjectConfirmation.BEARER;
        } else if (STSConstants.PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            /*
            The OnBehalfOf element defined in WS-Trust is used to indicate that the STS is being asked to issue a
            token OnBehalfOf another party, which is idiom which matches the STS issuing a SAML2 SV assertion.
             */
            if (tokenProviderParameters.getTokenRequirements().getOnBehalfOf() != null) {
                return SAML2SubjectConfirmation.SENDER_VOUCHES;
            } else {
                return SAML2SubjectConfirmation.HOLDER_OF_KEY;
            }
        } else if (STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyType)) {
            /*
            The TokenGenerationService does not, as of now, support HoK assertions with KeyInfo state in the SubjectConfirmationData
            corresponding to symmetric keys.
             */
            throw new AMSTSRuntimeException(ResourceException.NOT_SUPPORTED, "Issuing SAML2 assertions with symmetric KeyInfo" +
                    "in the SubjectConfirmationData of HoK assertions is currently not supported.");
        } else {
            String message = "Unexpected keyType in SoapSamlTokenProvider#determineSubjectConfirmation: " + keyType;
            logger.error(message);
            throw new AMSTSRuntimeException(ResourceException.BAD_REQUEST, message);
        }
    }

    /*
    This method must return the AuthnContextClassRef, a set of values defined in SAML2, included in the assertion generated
    by the TokenGenerationService, which specify the authentication performed as part of issuing the assertion. Essentially
    this value tells the relying party how the assertion subject was authenticated.

    A SAML2 assertion will be generated under two circumstances:
    1. as part of token transformation defined in the validate operation
    2. as part of an issue operation

    For case #1, the type of the validated token must be determined - accessed via the TokenRequirements in the TokenProviderParameters
    For case #2, I imagine that it is possible to obtain the validated token from the Security-Policy enforcing interceptors
    traversed during the Issue operation invocation.

    Firstly, I have to determine what invocation I am dealing with - presumably this is possible by looking at the
    tokens in the TokenRequirements.
     */
    private String getAuthnContextClassRef(TokenProviderParameters tokenProviderParameters) {
        TokenRequirements tokenRequirements = tokenProviderParameters.getTokenRequirements();
        if (tokenRequirements.getRenewTarget() != null) {
            //TODO: I am dealing with a renew - need to handle
            return "bobo";
        } else if (tokenRequirements.getValidateTarget() != null) {
            /*
            A validate invocation - so I need to parse out the validate token and determine its TokenType to pass to the
            XmlTokenAuthnContextMapper.
             */
            ReceivedToken receivedToken = tokenRequirements.getValidateTarget();
            /*
            The set of input tokens we support is currently limited to:
             1. UNTs
             2. OpenAM tokens - will be represented as a DOM Element

             When we support X509 Certificates, a new branch has to be added here. Not sure whether a X509Cert is represented
             as a JAXBElement or a DOM Element. See ReceivedToken for details.
             */
            if (receivedToken.isUsernameToken()) {
                if (receivedToken.getToken() instanceof UsernameToken) {
                    return authnContextMapper.getAuthnContext(TokenType.USERNAME, ((UsernameToken)receivedToken.getToken()).getElement());
                } else {
                    String message = "Unexpected type for a ReceivedToken which reports that it is a UsernameToken: the type: "
                            + receivedToken.getToken().getClass().getCanonicalName() + "; The actual token: " + receivedToken.getToken();
                    logger.error(message);
                    throw new AMSTSRuntimeException(ResourceException.NOT_SUPPORTED, message);
                }
            } else if (receivedToken.isDOMElement()) {
                /*
                Right now, this can only mean we are dealing with an OpenAMSession token. Attempt to marshal with
                the amSessionTokenXmlMarshaller, just to be sure.
                 */
                Element tokenElement = (Element)receivedToken.getToken();
                try {
                    amSessionTokenXmlMarshaller.fromXml(tokenElement);
                    return authnContextMapper.getAuthnContext(TokenType.OPENAM, tokenElement);
                } catch (TokenMarshalException e) {
                    /*
                    Note it seems that we could enter this branch because the CXF-STS does not distinguish token validators
                    for status validation, and token validators for the input in a transformation operation. Thus if a
                    deployed STS instance supports a set of token validation operations which is a superset of the input
                    set in token transformation operations, then this branch could be entered.
                    TODO: see of this contingency could be obviated by distinguishing the status validators from the
                    transformation validators.
                     */
                    String message = "Unexpected state in SoapSamlTokenProivder#getAuthnContextClassRef: the ReceivedToken" +
                            " in the validateTarget is a DOM Element, but cannot be marshaled to an OpenAM Session token. " +
                            " This means that the validate operation is being invoked with an unsupported token type. " +
                            "The token element " + tokenElement;
                    logger.error(message);
                    throw new AMSTSRuntimeException(ResourceException.BAD_REQUEST, message);
                }
            } else {
                String message = "Unexpected validateTarget token in SoapSamlTokenProvider#getAuthnContextClassRef - " +
                        "the token is neither a DOM Element or a UNT. The token: " + receivedToken.getToken();
                logger.error(message);
                throw new AMSTSRuntimeException(ResourceException.NOT_SUPPORTED, message);
            }

        } else if (tokenRequirements.getCancelTarget() != null) {
            /*
            Should not enter this block, as Cancel operation was never bound in the STSEndpoint. Log an throw an exception
            if this occurs, as it is unexpected.
             */
            String message = "Unexpected state in SoapSamlTokenProvider: TokenProviderParameters has a non-null cancelTarget " +
                    "in the TokenRequirments. A cancel operation is not bound, so this state is unexpected!";
            logger.error(message);
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR, message);
        } else {
            /*
            Here we must be dealing with an Issue operation, so I need to obtain the token validated by the SecurityPolicy
            bindings protecting the issue operation.
             */
            MessageContext messageContext = tokenProviderParameters.getWebServiceContext().getMessageContext();
//            messageContext.
            final Message currentMessage = PhaseInterceptorChain.getCurrentMessage();
            Object obj = currentMessage.get(Message.IN_INTERCEPTORS);
            /*
            now go through and see if any are WSSecurityTokenHolder
            WSSecurityTokenHolder holder
            TODO: need to debug to figure this out.
            */
            return "bobo";
        }
    }

    /*
    Throw TokenCreationException as threadLocalAMTokenCache.getAMToken throws a TokenCreationException. Let caller above
    map that to an AMSTSRuntimeException.
     */
    private String getAssertion(TokenProviderParameters tokenProviderParameters, String authnContextClassRef,
                                SAML2SubjectConfirmation subjectConfirmation) throws TokenCreationException {
        switch (subjectConfirmation) {
            case BEARER:
                return tokenGenerationServiceConsumer.getSAML2BearerAssertion(threadLocalAMTokenCache.getAMToken(),
                        stsInstanceId, realm, NULL_SP_ACS_URL, authnContextClassRef);
            case SENDER_VOUCHES:
                return tokenGenerationServiceConsumer.getSAML2SenderVouchesAssertion(threadLocalAMTokenCache.getAMToken(),
                        stsInstanceId, realm, authnContextClassRef);
            case HOLDER_OF_KEY:
                ReceivedKey receivedKey = tokenProviderParameters.getKeyRequirements().getReceivedKey();
                X509Certificate certificate = receivedKey.getX509Cert();
                if (certificate == null) {
                    String exceptionMessage = "The ReceivedKey instance in the KeyRequirements has a null X509Cert. Thus the " +
                            "ProofTokenState necessary to consume the TokenGenerationService cannot be created.";
                    logger.error(exceptionMessage + " PublicKey in the ReceivedToken: " + receivedKey.getPublicKey());
                    throw new TokenCreationException(ResourceException.BAD_REQUEST, exceptionMessage);
                }
                ProofTokenState proofTokenState;
                try {
                    proofTokenState = ProofTokenState.builder().x509Certificate(certificate).build();
                } catch (TokenMarshalException e) {
                    String message = "In SoapSamlTokenProvider#getAssertion, could not marshal X509Cert in ReceivedKey " +
                            "into ProofTokenState: " + e;
                    logger.error(message, e);
                    throw new TokenCreationException(ResourceException.BAD_REQUEST, message);
                }
                return tokenGenerationServiceConsumer.getSAML2HolderOfKeyAssertion(threadLocalAMTokenCache.getAMToken(),
                        stsInstanceId, realm, authnContextClassRef, proofTokenState);
        }
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                "Unexpected SAML2SubjectConfirmation in AMSAMLTokenProvider: " + subjectConfirmation);
    }
}
