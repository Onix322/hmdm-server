package com.hmdm.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.User;
import com.hmdm.util.PasswordUtil;

/**
 * Authenticates a user using an OIDC ID token.
 *
 * @param user The user attempting to authenticate
 * @param tokenOrPassword The raw password string for local auth, or a JWT string for OIDC auth
 * @return {@code true} if authentication succeeds, {@code false} otherwise
 */
@Singleton
public class OIDCAuth implements HmdmAuthInterface {

    private UnsecureDAO userDAO;

    @Inject
    public OIDCAuth(UnsecureDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public User findUser(String login) {
        User user = userDAO.findByLogin(login);
        return user;
    }

    @Override
    public boolean authenticate(User user, String password) {
        boolean match = PasswordUtil.passwordMatch(password, user.getPassword());
        if (!match) {
            userDAO.setUserLoginFailTime(user, System.currentTimeMillis());
        }
        return match;
    }
}
