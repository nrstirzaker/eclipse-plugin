/*
 * CODENVY CONFIDENTIAL
 * ________________
 * 
 * [2012] - [2014] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.eclipse.core.services;

import com.codenvy.eclipse.core.model.CodenvyCredentials;
import com.codenvy.eclipse.core.model.CodenvyToken;

/**
 * Codenvy authentication service contract.
 * 
 * @author Kevin Pollet
 * @author Stéphane Daviet
 */
public interface AuthenticationService extends RestService {
    /**
     * Authenticates the user on the Codenvy platform.
     * 
     * @param credentials the Codenvy credentials.
     * @return the authentication token.
     * @throws NullPointerException if credentials parameter is {@code null}.
     */
    CodenvyToken login(CodenvyCredentials credentials);
}
