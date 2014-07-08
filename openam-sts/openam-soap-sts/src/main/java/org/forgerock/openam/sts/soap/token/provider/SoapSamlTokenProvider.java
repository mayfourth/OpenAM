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

import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.request.ReceivedKey;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.ws.security.WSConstants;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


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
                                 Logger logger) {
        this.tokenGenerationServiceConsumer = tokenGenerationServiceConsumer;
        this.amSessionInvalidator = amSessionInvalidator;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.stsInstanceId = stsInstanceId;
        this.realm = realm;
        this.xmlUtilities = xmlUtilities;
        this.authnContextMapper = authnContextMapper;
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

    For case #1, the type of the validated token must be determined
     */
    private String getAuthnContextClassRef(TokenProviderParameters tokenProviderParameters) {
        return null;
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

                //TODO: how to marshal receivedKey into ProofTokenState to replace the null below.
                return tokenGenerationServiceConsumer.getSAML2HolderOfKeyAssertion(threadLocalAMTokenCache.getAMToken(),
                        stsInstanceId, realm, authnContextClassRef, null);
        }
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                "Unexpected SAML2SubjectConfirmation in AMSAMLTokenProvider: " + subjectConfirmation);
    }
}
