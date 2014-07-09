/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: LDAP.java,v 1.17 2010/01/25 22:09:16 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2014 ForgeRock, Inc.
 */
package org.forgerock.openam.authentication.modules.scripted;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.client.HttpClient;
import org.forgerock.http.client.HttpClientFactory;
import org.forgerock.http.client.request.HttpClientRequest;
import org.forgerock.http.client.request.HttpClientRequestFactory;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.StandardScriptEvaluator;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.utils.IOUtils;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * An authentication module that allows users to authenticate via a scripting language
 */
public class Scripted extends AMLoginModule {
    public static final String ATTR_NAME_PREFIX = "iplanet-am-auth-scripted-";
    public static final String CLIENT_SCRIPT_ATTR_NAME = ATTR_NAME_PREFIX + "client-script";
    public static final String CLIENT_SCRIPT_ENABLED_ATTR_NAME = ATTR_NAME_PREFIX + "client-script-enabled";
    public static final String SCRIPT_TYPE_ATTR_NAME = ATTR_NAME_PREFIX + "script-type";
    public static final String SERVER_SCRIPT_ATTRIBUTE_NAME = ATTR_NAME_PREFIX + "server-script";
    public static final String CLIENT_SCRIPT_EQUALS_SYMBOL_ATTR_NAME = ATTR_NAME_PREFIX +
            "client-script-equals-symbol";
    public static final String CLIENT_SCRIPT_DELIMITER_SYMBOL_ATTR_NAME = ATTR_NAME_PREFIX +
            "client-script-delimiter-symbol";
    public static final String SCRIPT_NAME = "server-side-script";

    public static final String JAVA_SCRIPT_LABEL = "Java Script";
    public static final String GROOVY_LABEL = "Groovy";
    private final static int STATE_BEGIN = 1;

    private final static int STATE_RUN_SCRIPT = 2;
    public static final String STATE_VARIABLE_NAME = "authState";
    private static final String SUCCESS_ATTR_NAME = "SUCCESS";
    public static final int SUCCESS_VALUE = -1;
    private static final String FAILED_ATTR_NAME = "FAILED";
    public static final int FAILURE_VALUE = -2;
    public static final String USERNAME_VARIABLE_NAME = "username";
    public static final String HTTP_CLIENT_REQUEST_VARIABLE_NAME = "httpClientRequest";
    public static final String HTTP_CLIENT_VARIABLE_NAME = "httpClient";
    public static final String LOGGER_VARIABLE_NAME = "logger";
    public static final String IDENTITY_REPOSITORY = "idRepository";
    public static final String CLIENT_SCRIPT_OUTPUT_DATA_PARAMETER_NAME = "clientScriptOutputData";
    public static final String CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME = "clientScriptOutputData";
    public static final String REQUEST_DATA_VARIABLE_NAME = "requestData";

    public static final String UTILITY_FUNCTIONS_FILE_CLASS_PATH = "/utilityFunctions.js";

    private String userName;
    private String clientSideScript;
    private boolean clientSideScriptEnabled;
    private ScriptObject serverSideScript;
    private ScriptEvaluator scriptEvaluator;
    public Map moduleConfiguration;

    /** Debug logger instance used by scripts to log error/debug messages. */
    private static final Debug DEBUG = Debug.getInstance("amScript");

    final HttpClientFactory httpClientFactory = InjectorHolder.getInstance(HttpClientFactory.class);
    final HttpClientRequestFactory httpClientRequestFactory = InjectorHolder.getInstance(HttpClientRequestFactory.class);
    private HttpClient httpClient;
    private HttpClientRequest httpClientRequest;
    private ScriptIdentityRepository identityRepository;
    private String equalsSymbol;
    private String delimiterSymbol;

    private Map sharedState;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        this.sharedState = sharedState;

        userName = (String) sharedState.get(getUserKey());
        moduleConfiguration = options;
        clientSideScript = getClientSideScript();
        scriptEvaluator = getScriptEvaluator();
        serverSideScript = getServerSideScript();
        clientSideScriptEnabled = getClientSideScriptEnabled();
        httpClient = getHttpClient();
        httpClientRequest = getHttpRequest();
        identityRepository  = getScriptIdentityRepository();
        equalsSymbol = getConfigValue(CLIENT_SCRIPT_EQUALS_SYMBOL_ATTR_NAME);
        delimiterSymbol = getConfigValue(CLIENT_SCRIPT_DELIMITER_SYMBOL_ATTR_NAME);
    }

    private ScriptIdentityRepository getScriptIdentityRepository() {
        return new ScriptIdentityRepository(getAmIdentityRepository());
    }

    private AMIdentityRepository getAmIdentityRepository() {
        return getAMIdentityRepository(getRequestOrg());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {

        switch (state) {

            case STATE_BEGIN:
                if (!clientSideScriptEnabled) {
                    clientSideScript = " ";
                }

                substituteUIStrings();

                return STATE_RUN_SCRIPT;

            case STATE_RUN_SCRIPT:
                Bindings scriptVariables = new SimpleBindings();
                scriptVariables.put(REQUEST_DATA_VARIABLE_NAME, getScriptHttpRequestWrapper());
                Map clientScriptOutputDataMap = getClientScriptOutputDataMap();
                scriptVariables.put(CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME, clientScriptOutputDataMap);
                scriptVariables.put(LOGGER_VARIABLE_NAME, DEBUG);
                scriptVariables.put(STATE_VARIABLE_NAME, state);
                scriptVariables.put(USERNAME_VARIABLE_NAME, userName);
                scriptVariables.put(SUCCESS_ATTR_NAME, SUCCESS_VALUE);
                scriptVariables.put(FAILED_ATTR_NAME, FAILURE_VALUE);
                scriptVariables.put(HTTP_CLIENT_VARIABLE_NAME, httpClient);
                scriptVariables.put(HTTP_CLIENT_REQUEST_VARIABLE_NAME, httpClientRequest);
                scriptVariables.put(IDENTITY_REPOSITORY, identityRepository);

                try {
                    scriptEvaluator.evaluateScript(serverSideScript, scriptVariables);
                } catch (ScriptException e) {
                    DEBUG.message("Error running server side scripts", e);
                    throw new AuthLoginException("Error running script");
                }

                state = ((Number) scriptVariables.get(STATE_VARIABLE_NAME)).intValue();
                userName = (String) scriptVariables.get(USERNAME_VARIABLE_NAME);
                sharedState.put(CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME, clientScriptOutputDataMap);

                if (state != SUCCESS_VALUE) {
                    throw new AuthLoginException("Authentication failed");
                }

                return state;
            default:
                throw new AuthLoginException("Invalid state");
        }

    }

    private ScriptObject getServerSideScript() {
        SupportedScriptingLanguage scriptType = getScriptType();
        String rawScript = getRawServerSideScript();

        return new ScriptObject(SCRIPT_NAME, rawScript, scriptType, null);
    }

    private ScriptEvaluator getScriptEvaluator() {
        return InjectorHolder.getInstance(StandardScriptEvaluator.class);
    }

    private HttpClient getHttpClient() {
       return httpClientFactory.createHttpClient();

    }

    private HttpClientRequest getHttpRequest() {
       return httpClientRequestFactory.createRequest();
    }

    private String getClientSideScript() {
        String clientSideScript = getConfigValue(CLIENT_SCRIPT_ATTR_NAME);

        if(clientSideScript == null) {
            clientSideScript = "";
        }

        return clientSideScript;
    }

    private String getRawServerSideScript() {
        String serverSideScript = getConfigValue(SERVER_SCRIPT_ATTRIBUTE_NAME);

        if (serverSideScript == null) {
            serverSideScript = "";
        }

        return serverSideScript;
    }

    private Map getClientScriptOutputDataMap() {
        String clientScriptOutputData = getScriptHttpRequestWrapper().
                getParameter(CLIENT_SCRIPT_OUTPUT_DATA_PARAMETER_NAME);
        Map<String, String> dataMap = new LinkedHashMap<String, String>();

        StringTokenizer tokenizer = new StringTokenizer(clientScriptOutputData, delimiterSymbol);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String keyValueArray[] = token.split(equalsSymbol);
            String key = keyValueArray[0];
            String value = "";
            if (keyValueArray.length == 2) {
                value = keyValueArray[1];
            }
            dataMap.put(key, value);
        }

        return dataMap;
    }

    private String getConfigValue(String attributeName) {
        return CollectionHelper.getMapAttr(moduleConfiguration, attributeName);
    }

    private ScriptHttpRequestWrapper getScriptHttpRequestWrapper() {
        return new ScriptHttpRequestWrapper(getHttpServletRequest());
    }

    private void substituteUIStrings() throws AuthLoginException {
        replaceCallback(STATE_RUN_SCRIPT, 0, getHiddenFieldCallback());
        replaceCallback(STATE_RUN_SCRIPT, 1, getScriptOutputUtilityFunctionsCallback());
        replaceCallback(STATE_RUN_SCRIPT, 2, getScriptAndSelfSubmitCallback());
    }

    private Callback getHiddenFieldCallback() {
        TextOutputCallback hiddenFieldCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "" +
                "<input type=\"hidden\" name=\"" + CLIENT_SCRIPT_OUTPUT_DATA_PARAMETER_NAME + "\" " +
                "id=\"" + CLIENT_SCRIPT_OUTPUT_DATA_PARAMETER_NAME + "\" " +
                "value=\"\" />");

        return hiddenFieldCallback;
    }

    private Callback getScriptOutputUtilityFunctionsCallback() throws AuthLoginException {
        String equalsSymbolJsStatement = "var equalsSymbol='" + equalsSymbol + "';\n";
        String delimiterSymbolJsStatement = "var delimiterSymbol='" + delimiterSymbol + "';\n";

        String utilityFunctionsJs = "";
        try {
            utilityFunctionsJs = IOUtils.getFileContentFromClassPath(UTILITY_FUNCTIONS_FILE_CLASS_PATH);
        } catch (IOException e) {
            throw new AuthLoginException("Could not find javascript utility functions expected at " +
                    UTILITY_FUNCTIONS_FILE_CLASS_PATH);
        }

        ScriptTextOutputCallback scriptOutputUtilityFunctionsCallback =
                new ScriptTextOutputCallback(equalsSymbolJsStatement + delimiterSymbolJsStatement + utilityFunctionsJs);

        return scriptOutputUtilityFunctionsCallback;
    }

    private Callback getScriptAndSelfSubmitCallback() {
        ScriptTextOutputCallback scriptAndSelfSubmitCallback = new ScriptTextOutputCallback(clientSideScript + "\n" +
                "prepareScriptOutputDataForSubmission();\n" +
                "document.forms[0].submit()");

        return scriptAndSelfSubmitCallback;
    }

    private SupportedScriptingLanguage getScriptType() {
        String scriptTypeVariable = getConfigValue(SCRIPT_TYPE_ATTR_NAME);

        if (JAVA_SCRIPT_LABEL.equals(scriptTypeVariable)) {
            return SupportedScriptingLanguage.JAVASCRIPT;
        } else if (GROOVY_LABEL.equals(scriptTypeVariable)) {
            return SupportedScriptingLanguage.GROOVY;
        }

        return null;
    }

    private boolean getClientSideScriptEnabled() {
        String clientSideScriptEnabled = getConfigValue(CLIENT_SCRIPT_ENABLED_ATTR_NAME);
        return Boolean.parseBoolean(clientSideScriptEnabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getPrincipal() {
        if(userName == null) {
            DEBUG.message("Warning: username is null");
        }

        return new ScriptedPrinciple(userName);
    }
}
