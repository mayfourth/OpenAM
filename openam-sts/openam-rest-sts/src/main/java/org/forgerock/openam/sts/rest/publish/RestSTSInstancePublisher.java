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

package org.forgerock.openam.sts.rest.publish;

import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;

import java.util.List;

/**
 * Defines the interface consumed to publish a Rest STS instance, and to remove this instance once its functionality
 * should no longer be exposed.
 *
 * It may well be that this interface should be enhanced to handle a GET to the RestSTSPublishService, which will
 * return the RestSTSInstanceConfig instances corresponding to all published REST STS instances. This method would then
 * be consumed by a servlet-context-listener (or similar startup context) associated with the REST STS deployment. This
 * would allow REST STS instances to re-constitute themselves after a server restart regardless of whether they are deployed
 * locally (in the OpenAM .war) or remotely (in their own .war).
 */
public interface RestSTSInstancePublisher {
    /**
     * Publish the Rest STS instance specified by the instanceConfig parameter. Publishing a Rest STS instance
     * means persisting it to the SMS and exposing it via CREST.
     * @param instanceConfig The configuration state for the to-be-published Rest STS instance.
     * @param instance The RestSTS instance to be exposed
     * @param republish Determines whether this is an initial publish, or a re-publish. If re-publish, instanceConfig
     *                  not written to the SMS.
     * @return the urlElement, including the realm, at which the Rest STS instance has been published. TODO: should this
     * include the deployment information for the Rest STS service as well? Probably should
     * @throws STSPublishException
     */
    String publishInstance(RestSTSInstanceConfig instanceConfig, RestSTS instance, boolean republish) throws STSPublishException;

    /**
     * Remove the Rest STS instance from the SMS, and from the CREST router.
     * @param stsId The sts id, obtained from RestSTSInstanceConfig#getDeploymentSubPath
     * @param realm The realm in which the Rest STS is to be deployed.
     *
     * Note that the subPath and realm constitute the unique identifier for a given Rest STS instance.
     * @throws STSPublishException
     */
    void removeInstance(String stsId, String realm) throws STSPublishException;

    /**
     * Called to obtain the configuration elements corresponding to previously-published STS instances.
     * @return The RestSTSInstanceConfig instances corresponding
     * to published STS instances.
     * @throws STSPublishException if exception encountered obtaining persisted instance state
     */
    List<RestSTSInstanceConfig> getPublishedInstances() throws STSPublishException;

    RestSTSInstanceConfig getPublishedInstance(String stsId, String realm) throws STSPublishException;

    /**
     * Called by the RestSTSRepublishServlet upon startup to re-publish previously-published Rest STS instances.
     * @throws STSPublishException if exception encountered obtaining and re-publishing previously published instances.
     * Note that exception will be thrown only if exception encountered which prevents the re-publishing of all
     * previously-published instances. Exceptions encountered re-publishing individual instances will only be logged.
     */
    void republishExistingInstances() throws STSPublishException;
}
