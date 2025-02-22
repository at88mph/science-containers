/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2025.                            (c) 2025.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 ************************************************************************
 */
package org.opencadc.security.sso;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.RequestAgent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.security.SsoAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;

import javax.servlet.http.Cookie;

/**
 * The {@code TokenRelay} class implements the {@link SsoAdapter} interface to handle
 * Single Sign-On (SSO) authentication within the CADC environment.
 * 
 * <p>This class is responsible for retrieving the authentication token from the SSO cookie
 * and setting the authorization credential for HTTP service inputs.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * TokenRelay tokenRelay = new TokenRelay();
 * Token token = tokenRelay.getAuthToken();
 * if (token != null) {
 *     System.out.println("Token ID: " + token.getId());
 * }
 * }</pre>
 * 
 * <p>Environment variables used:</p>
 * <ul>
 *   <li>CADC_SSO_COOKIE_NAME: The name of the SSO cookie (default: "CADC_SSO").</li>
 *   <li>CADC_SSO_COOKIE_DOMAIN: The domain of the SSO cookie (default: ".canfar.net").</li>
 *   <li>CADC_ALLOWED_DOMAIN: The domain of the downstream service (default: ".canfar.net").</li>
 * </ul>
 * 
 * <p>Methods:</p>
 * <ul>
 *   <li>{@link #getAuthToken()}: Retrieves the authentication token from the SSO cookie.</li>
 *   <li>{@link #setAuthCredential(HttpServiceInput)}: Sets the authorization credential for the given HTTP service input.</li>
 *   <li>{@link #getUserInfo()}: Retrieves the user information associated with the current session.</li>
 *   <li>{@link #getRequestAgent()}: Retrieves the request agent from the server context.</li>
 * </ul>
 */
public class TokenRelay implements SsoAdapter {

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private Token token = null;

    /**
     * SSO Cookie Properties
     * SSO_COOKIE_NAME: The name of the SSO cookie.
     * SSO_COOKIE_DOMAIN: The domain of the SSO cookie.
     */
    private static final String SSO_COOKIE_NAME = System.getenv().getOrDefault("CADC_SSO_COOKIE_NAME", "CADC_SSO");
    private static final String SSO_COOKIE_DOMAIN = System.getenv().getOrDefault("CADC_SSO_COOKIE_DOMAIN", ".canfar.net");
    
    /**
     * Downstream Service Properties
     * ALLOWED_DOMAIN: The domain of the downstream service.
     */
    private static final String ALLOWED_DOMAIN = System.getenv().getOrDefault("CADC_ALLOWED_DOMAIN", ".canfar.net");

    /**
     * Retrieves the authentication token from the SSO cookie.
     * 
     * This method retrieves the SSO cookie from the request agent and, if the cookie is not null,
     * extracts the token value and domain. If the domain matches the expected SSO cookie domain,
     * a new {@link Token} object is created with the token value.
     * 
     * @return Token The authentication token retrieved from the SSO cookie.
     */
    @Override
    public Token getAuthToken() {
        token = null;
        try {
            RequestAgent agent = getRequestAgent();
            Cookie ssoCookie = agent.getCookie(SSO_COOKIE_NAME);

            if (ssoCookie != null) {
                String ssoToken = ssoCookie.getValue(); // Get the value of the cookie
                String cookieDomain = ssoCookie.getDomain(); // Get the domain of the cookie
                if (!SSO_COOKIE_DOMAIN.endsWith(cookieDomain) || cookieDomain == null) {
                    LOGGER.info("SSO Token found, but invalid domain " + cookieDomain);
                    return null;
                }
                token = new Token(ssoToken);
                LOGGER.info("Retrieved SSO Token for " + cookieDomain);
            }
            else{
                LOGGER.info("SSO Token not found");
                LOGGER.info("SSO Token not found in Cookie Name: " + SSO_COOKIE_NAME);
            }

        }
        catch (Exception error){
            LOGGER.error(error);
        }
        return token;
    }
    
    
    /**
     * Sets the authorization credential for the given HTTP service input.
     * 
     * This method retrieves an authentication token and, if the token is not null
     * and the request URL requires an authorization credential, sets the "Authorization"
     * header of the HTTP service input to "Bearer " followed by the token ID.
     * 
     * @param inputs The HTTP service input for which the authorization credential is to be set.
     */
    @Override
    public void setAuthCredential(HttpServiceInput inputs) {
        Token token = getAuthToken();
        String requestURL = inputs.getRequestUrl();
        Boolean allowed = SsoAdapter.requireAuthCredential(requestURL, ALLOWED_DOMAIN);
        if (token != null && token.getId() != null && allowed) {
            inputs.setHeader("Authorization", "Bearer " + token.getId());
        }
    }

    /**
     * Retrieves the user information associated with the current session.
     *
     * @return Default implementation returns an empty {@link UserInfo} object.
     */
    @Override
    public UserInfo getUserInfo() {
        // Return default UserInfo with no user-specific data
        return new UserInfo();
    }

    /**
     * Retrieves the request agent from the server context. This method is package-private
     * to allow for testing with a mock request agent.
     * 
     * @return RequestAgent The request agent associated with the current request.
     */
    RequestAgent getRequestAgent() {
        return ServerContext.getRequestOwner().getRequestAgent();
    }
}
