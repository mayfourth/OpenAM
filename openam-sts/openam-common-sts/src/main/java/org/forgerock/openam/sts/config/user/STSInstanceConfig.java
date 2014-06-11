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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.config.user;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.MapMarshallUtils;
import org.forgerock.util.Reject;

import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Base class encapsulating STS configuration state common to both the REST and SOAP STS. A builder builds this
 * base class, and is invoked by the builders in the REST/SOAP sublcasses.
 * For an explanation of what's going on with the builders in this class and its subclasses,
 * see https://weblogs.java.net/blog/emcmanus/archive/2010/10/25/using-builder-pattern-subclasses
 *
 * Also attempted to marshal the RestSTSInstanceConfig to/from json with the jackson ObjectMapper. But I was adding
 * @JsonSerialize and @JsonDeserialize annotations, and because builder-based classes don't expose ctors which
 * take the complete field set, I would have to create @JsonCreator instances which would have to pull all of the
 * values out of a map anyway, which is 75% of the way towards a hand-rolled json marshalling implementation based on
 * json-fluent. So a hand-rolled implementation it is. See toJson and fromJson methods.
 */
public class STSInstanceConfig {
    /*
    Define the names of fields to aid in json marshalling. Note that these names match the names of the AttributeSchema
    entries in restSTS.xml, as this aids in marshalling an instance of this class into the attribute map needed for
    SMS persistence.
     */
    protected static final String AM_DEPLOYMENT_URL = "am-deployment-url";
    protected static final String KEYSTORE_CONFIG =  "keystore-config";
    protected static final String ISSUER_NAME = "issuer-name";
    protected static final String SAML2_CONFIG = "saml2-config";

    protected final String amDeploymentUrl;
    protected final KeystoreConfig keystoreConfig;
    protected final String issuerName;
    protected final SAML2Config saml2Config;

    public static abstract class STSInstanceConfigBuilderBase<T extends STSInstanceConfigBuilderBase<T>> {
        private String amDeploymentUrl;
        private KeystoreConfig keystoreConfig;
        private String issuerName;
        private SAML2Config saml2Config;


        protected abstract T self();

        public T amDeploymentUrl(String url) {
            this.amDeploymentUrl = url;
            return self();
        }

        public T keystoreConfig(KeystoreConfig keystoreConfig) {
            this.keystoreConfig = keystoreConfig;
            return self();
        }

        public T issuerName(String issuerName) {
            this.issuerName = issuerName;
            return self();
        }

        public T saml2Config(SAML2Config saml2Config) {
            this.saml2Config = saml2Config;
            return self();
        }

        public STSInstanceConfig build() {
            return new STSInstanceConfig(this);
        }
    }

    public static class STSInstanceConfigBuilder extends STSInstanceConfigBuilderBase<STSInstanceConfigBuilder> {
        @Override
        protected STSInstanceConfigBuilder self() {
            return this;
        }
    }

    protected STSInstanceConfig(STSInstanceConfigBuilderBase<?> builder) {
        amDeploymentUrl = builder.amDeploymentUrl;
        keystoreConfig = builder.keystoreConfig;
        issuerName = builder.issuerName;
        //can be null if STS does not issue SAML tokens - but if SAML2 tokens only output, this must be non-null. TODO:
        saml2Config = builder.saml2Config;
        Reject.ifNull(keystoreConfig, "KeystoreConfig cannot be null");
        Reject.ifNull(issuerName, "Issuer name cannot be null");
        Reject.ifNull(amDeploymentUrl, "AM deployment url cannot be null");
    }

    /**
     * @return  The crypto-context necessary to decrypt/trust/sign/encrypt the tokens validated and generated by this
     *          STS instance.
     */
    public KeystoreConfig getKeystoreConfig() {
        return keystoreConfig;
    }

    /**
     * @return  The issuerName in tokens (e.g. SAML2) generated by the STS.
     */
    public String getIssuerName() {
        return issuerName;
    }

    /**
     * @return  The String corresponding to the OpenAM deployment url. (May go away, as the REST-STS will likely be
     *          co-deployed with OpenAM).
     */
    public String getAMDeploymentUrl() {
        return amDeploymentUrl;
    }

    /**
     *
     * @return The SAML2Config object which specifies the state necessary for STS-instance-specific SAML2 assertions to
     * be generated. This state is used by the token generation service.
     */
    public SAML2Config getSaml2Config() {
        return saml2Config;
    }

    public static STSInstanceConfigBuilderBase<?> builder() {
        return new STSInstanceConfigBuilder();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("STSInstanceConfig instance:\n");
        sb.append('\t').append("KeyStoreConfig: ").append(keystoreConfig).append('\n');
        sb.append('\t').append("issuerName: ").append(issuerName).append('\n');
        sb.append('\t').append("amDeploymentUrl: ").append(amDeploymentUrl).append('\n');
        sb.append('\t').append("saml2Config: ").append(saml2Config).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof STSInstanceConfig) {
            STSInstanceConfig otherConfig = (STSInstanceConfig)other;
            return keystoreConfig.equals(otherConfig.getKeystoreConfig()) &&
                    issuerName.equals(otherConfig.getIssuerName()) &&
                    amDeploymentUrl.equals(otherConfig.getAMDeploymentUrl()) &&
                    ((saml2Config != null ? saml2Config.equals(otherConfig.getSaml2Config()) : (otherConfig.getSaml2Config() == null)));
        }
        return false;
    }

    public JsonValue toJson() {
        JsonValue jsonValue =  json(object(field(AM_DEPLOYMENT_URL, amDeploymentUrl),
                field(KEYSTORE_CONFIG, keystoreConfig.toJson()), field(ISSUER_NAME, issuerName)));
        if (saml2Config == null) {
            return jsonValue;
        } else {
            jsonValue.add(SAML2_CONFIG, saml2Config.toJson());
            return jsonValue;
        }
    }

    public static STSInstanceConfig fromJson(JsonValue json) {
        STSInstanceConfigBuilderBase builder =  STSInstanceConfig.builder()
                .amDeploymentUrl(json.get(AM_DEPLOYMENT_URL).asString())
                .keystoreConfig(KeystoreConfig.fromJson(json.get(KEYSTORE_CONFIG)))
                .issuerName(json.get(ISSUER_NAME).asString());
        final JsonValue samlConfig = json.get(SAML2_CONFIG);
        if (samlConfig.isNull()) {
            return builder.build();
        } else {
            return builder.saml2Config(SAML2Config.fromJson(samlConfig)).build();
        }
    }

    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Set<String>> attributes = MapMarshallUtils.toSmsMap(toJson().asMap());
        /*
        the Map<String, Object> expected by the SMS is a flat structure - so I need to flatten all
        nested elements.
         */
        if (saml2Config != null) {
            attributes.remove(SAML2_CONFIG);
            attributes.putAll(saml2Config.marshalToAttributeMap());
        }
        attributes.remove(KEYSTORE_CONFIG);
        attributes.putAll(keystoreConfig.marshalToAttributeMap());
        return attributes;
    }

    public static STSInstanceConfig marshalFromAttributeMap(Map<String, Set<String>> attributeMap) {
        Map<String, Object> jsonAttributes = MapMarshallUtils.toJsonValueMap(attributeMap);
        /*
        Here I need to un-flatten the SAML2Config and keystoreConfig state.
         */
        KeystoreConfig keystoreConfig = KeystoreConfig.marshalFromAttributeMap(attributeMap);
        jsonAttributes.put(KEYSTORE_CONFIG, keystoreConfig.toJson());
        /*
        If SAML2Config state is not present, null will be returned. That will tell me that no SAML2Config
        had been present initially.
         */
        SAML2Config saml2Config = SAML2Config.marshalFromAttributeMap(attributeMap);
        if (saml2Config != null) {
            jsonAttributes.put(SAML2_CONFIG, saml2Config.toJson());
        }
        return fromJson(new JsonValue(jsonAttributes));
    }
}
