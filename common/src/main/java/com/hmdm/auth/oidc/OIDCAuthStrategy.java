package com.hmdm.auth.oidc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.auth.AuthStrategy;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.User;

/**
 * Authenticates a user using an OIDC ID token.
 *
 * @param user The user attempting to authenticate
 * @param tokenOrPassword The raw password string for local auth, or a JWT string for OIDC auth
 * @return {@code true} if authentication succeeds, {@code false} otherwise
 */
@Singleton
public class OIDCAuthStrategy implements AuthStrategy<OidcAuthCredentials> {

    private UnsecureDAO userDAO;

    @Inject
    public OIDCAuthStrategy(UnsecureDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public User findUser(String login) {
        User user = userDAO.findByLogin(login);
        return user;
    }

    @Override
    public boolean authenticate(OidcAuthCredentials credentials) {
        System.out.println("[!!!!!! DEBUG TEMPORARY !!!!!] Using OIDC provider");
        return true;
    }
}
