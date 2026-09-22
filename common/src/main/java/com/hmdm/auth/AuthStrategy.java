package com.hmdm.auth;

import com.hmdm.persistence.domain.User;

/**
 * Defines a strategy contract for authenticating users using specific credential types.
 *
 * @param <T> the type of {@link AuthCredentials} handled by this strategy
 */
public interface AuthStrategy<T extends AuthCredentials> {

    /**
     * Finds a user entity associated with the provided login identifier.
     *
     * @param login the username or email address
     * @return the matched {@link User}, or {@code null} if not found
     */
    User findUser(String login);

    /**
     * Validates the provided credentials against the authentication system.
     *
     * @param credentials the credential details required for authentication
     * @return {@code true} if authentication succeeds, {@code false} otherwise
     */
    boolean authenticate(T credentials);
}
