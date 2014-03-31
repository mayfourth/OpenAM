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

package org.forgerock.openam.authentication.modules.oidc;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.authentication.modules.oidc.PrincipalMapper
 */
public class DefaultPrincipalMapper implements PrincipalMapper {
    private static Debug logger = Debug.getInstance("amAuth");
    /*
    This value should always be set to one, as we are returning the first result in the Set encapsulated in the
    IdSearchResult class. See lookupPrincipal below.
     */
    private static final int SINGLE_SEARCH_RESULT = 1;

    //TODO - unit test for this.
    public Map<String, Set<String>> getAttributesForPrincipalLookup(Map<String, String> localToJwtAttributeMapping,
                                                                    SignedJwt jwt,
                                                                    JwtClaimsSet jwtClaimsSet,
                                                                    URL profileServiceUrl) {
        Map<String, Set<String>> lookupAttributes = new HashMap<String, Set<String>>();
        for (Map.Entry<String, String> entry : localToJwtAttributeMapping.entrySet()) {
            if (jwtClaimsSet.isDefined(entry.getValue())) {
                //TODO: is this test necessary, or can I assume that jwts never have duplicate keys?
                if (!lookupAttributes.containsKey(entry.getKey())) {
                    Set<String> value = new HashSet<String>();
                    value.add(jwtClaimsSet.getClaim(entry.getValue()).toString());
                    lookupAttributes.put(entry.getKey(), value);
                } else {
                    lookupAttributes.get(entry.getKey()).add(jwtClaimsSet.getClaim(entry.getValue()).toString());
                }
            }
        }
        //TODO - hit profile service and map attributes!
        return lookupAttributes;
    }

    @Override
    public String lookupPrincipal(AMIdentityRepository idrepo, Map<String, Set<String>> searchAttributes) {
        if (searchAttributes == null || searchAttributes.isEmpty()) {
            logger.error("Search attributes empty in lookupPrincipal!");
            return null;
        }
        try {
            final IdSearchResults results = idrepo.searchIdentities(IdType.USER, "*", getSearchControl(searchAttributes));
            if (IdSearchResults.SUCCESS == results.getErrorCode()) {
                if (results.getSearchResults().size() == SINGLE_SEARCH_RESULT) {
                    //Even if IdSearchResults does not hit a match, we enter this block. Also the size of the resultSet does not
                    //seem accurate - it has a size of 1 even if no result is returned. Only testing iter.hasNext will determine if
                    // there actually are results.
                    Iterator<AMIdentity> iter = results.getSearchResults().iterator();
                    if (iter.hasNext()) {
                        return iter.next().getName();
                    } else {
                        logger.warning("In lookupPrincipal, result set has no results for searchAttributes: " + searchAttributes);
                    }
                } else {
                    logger.warning("In lookupPrincipal, result set has unexpected cardinality: " + results.getSearchResults().size());
                }
            } else {
                logger.warning("In lookupPrincipal, IdSearchResults returned non-success status: " + results.getErrorCode());
            }
        } catch (IdRepoException ex) {
            logger.error("DefaultPrincpalMapper.lookupPrincipal: Problem while  "
                    + "searching  for the user: " + ex, ex);
        } catch (SSOException ex) {
            logger.error("DefaultPrincpalMapper.lookupPrincipal: Problem while  "
                    + "searching  for the user: " + ex, ex);
        }
        return null;
    }


    private IdSearchControl getSearchControl(Map<String, Set<String>> searchAttributes) {
        IdSearchControl control = new IdSearchControl();
        control.setMaxResults(SINGLE_SEARCH_RESULT);
        control.setSearchModifiers(IdSearchOpModifier.OR, searchAttributes);
        return control;
    }
}
