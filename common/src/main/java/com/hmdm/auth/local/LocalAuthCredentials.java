package com.hmdm.auth.local;

import com.hmdm.auth.AuthCredentials;
import com.hmdm.persistence.domain.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Credentials implementation for local password-based authentication. Holds the target {@link User}
 * entity and the raw password provided by the client.
 */
@Getter
@Setter
@AllArgsConstructor
public class LocalAuthCredentials implements AuthCredentials {
    private User user;
    private String password;
}
