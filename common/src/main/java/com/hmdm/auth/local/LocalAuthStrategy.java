package com.hmdm.auth.local;

import com.google.inject.Inject;
import com.hmdm.auth.AuthStrategy;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.User;
import com.hmdm.util.PasswordUtil;

import javax.inject.Singleton;

/** Authentication strategy implementation for local user and password verification. */
@Singleton
public class LocalAuthStrategy implements AuthStrategy<LocalAuthCredentials> {

    private UnsecureDAO userDAO;

    @Inject
    public LocalAuthStrategy(UnsecureDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Finds a user entity matching the provided login (username) address.
     *
     * @param login the login username or email address
     * @return the matched {@link User}, or {@code null} if not found
     */
    @Override
    public User findUser(String login) {
        return userDAO.findByLoginOrEmail(login);
    }

    /**
     * Validates the provided password against the stored MD5 password hash and records failed
     * attempts.
     *
     * @param credentials the {@link LocalAuthCredentials} containing the user and MD5 hashed
     *     password
     * @return {@code true} if the password matches, {@code false} otherwise
     */
    @Override
    public boolean authenticate(LocalAuthCredentials credentials) {

        System.out.println("[!!!!!! DEBUG TEMPORARY !!!!!] Using Local auth");
        boolean match =
                PasswordUtil.passwordMatch(
                        credentials.getPassword(), credentials.getUser().getPassword());
        if (!match) {
            userDAO.setUserLoginFailTime(credentials.getUser(), System.currentTimeMillis());
        }
        return match;
    }
}
