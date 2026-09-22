package com.hmdm.auth.oidc;

import com.hmdm.auth.AuthCredentials;
import com.hmdm.persistence.domain.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Credentials implementation for OpenID Connect (OIDC) authentication. Holds the target {@link
 * User} entity and the retrieved ID or authorization token.
 */
@Getter
@Setter
@AllArgsConstructor
public class OidcAuthCredentials implements AuthCredentials {
    private User user;
    private String token;
}
