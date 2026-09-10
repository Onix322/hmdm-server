/*
 *
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hmdm.security.jwt;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.User;
import com.hmdm.security.SecurityContext;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filters incoming requests and sets up a security context for the request processing if a header
 * corresponding to a valid user is found.
 *
 * @author isv
 */
@Singleton
public class JWTFilter implements Filter {

    /** A name of HTTP request's "Authorization" header. */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * An authentication token provider used for validating and parsing the authentication tokens
     * provided by incoming request.
     */
    private final TokenProvider tokenProvider;

    private final UnsecureDAO userDAO;

    /**
     * Constructs new <code>JWTFilter</code> instance using the specified authentication token
     * provider.
     *
     * @param tokenProvider an authentication token provider used for validating and parsing the
     *     authentication tokens provided by incoming request.
     */
    @Inject
    public JWTFilter(TokenProvider tokenProvider, UnsecureDAO userDAO) {
        this.tokenProvider = tokenProvider;
        this.userDAO = userDAO;
    }

    /** Does nothing. */
    @Override
    public void init(FilterConfig filterConfig) {}

    /** Does nothing. */
    @Override
    public void destroy() {}

    /**
     * Intercepts the specified request. If a valid authentication token is provided by the
     * specified request then set-ups current security context with authenticated principal based on
     * the provided token.
     *
     * @param servletRequest an incoming request.
     * @param servletResponse an outgoing response.
     * @param filterChain a filter chain mapped to specified request.
     * @throws IOException if an I/O error occurs in filter chain.
     * @throws ServletException if an unexpected error occurs in filter chain.
     */
    @Override
    public void doFilter(
            ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String jwt = resolveToken(httpServletRequest);
        if (jwt != null && !jwt.trim().isEmpty() && this.tokenProvider.validateToken(jwt)) {
            User authUser = this.tokenProvider.getAuthentication(jwt);
            User dbUser = userDAO.findByLoginOrEmail(authUser.getLogin());
            if (dbUser == null
                    || dbUser.getAuthToken() == null
                    || !dbUser.getAuthToken().equals(authUser.getAuthToken())) {
                ((HttpServletResponse) servletResponse).sendError(403);
                return;
            }

            // Set-up the security context
            try {
                SecurityContext.init(dbUser);
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                SecurityContext.release();
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    /**
     * Gets the authentication token if any is provided by the specified request. Analyzes <code>
     * Authorization</code> request header.
     *
     * @param request an incoming request.
     * @return an authentication token provided by the specified request or <code>null</code> if
     *     there is no such token provided.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
