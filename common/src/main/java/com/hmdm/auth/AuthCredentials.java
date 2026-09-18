package com.hmdm.auth;

/**
 * Represents a generic contract for authentication credentials.
 *
 * <p>Defines the parameters required by different authentication strategies. For example:
 *
 * <ul>
 *   <li>Local authentication provides a {@link User} and password.
 *   <li>OIDC authentication provides a {@link User} and token.
 * </ul>
 */
public interface AuthCredentials {}
