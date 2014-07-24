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

package com.sun.identity.console.reststs.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;

import java.util.Map;
import java.util.Set;

/**
 * Provides the data necessary for the RestSTSHomeViewBean, the UI display element which displays published Rest STS
 * instances.
 */
public interface RestSTSModel extends AMModel {
    Set<String> getPublishedInstances(String realm) throws AMConsoleException;
    void deleteInstances(String realm, Set<String> instanceNames) throws AMConsoleException;
    RestSTSModelResponse createInstance(Map<String, Set<String>> configurationState, String realm) throws AMConsoleException;
    RestSTSModelResponse updateInstance(Map<String, Set<String>> configurationState, String realm, String instanceName)
            throws AMConsoleException;
    Map<String, Set<String>> getInstanceState(String realm, String instanceName) throws AMConsoleException;
    RestSTSModelResponse validateConfigurationState(Map<String, Set<String>> configurationState);
}
