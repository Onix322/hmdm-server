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

package com.hmdm.rest.resource;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.hmdm.auth.HmdmAuthInterface;
import com.hmdm.persistence.CustomerDAO;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Settings;
import com.hmdm.persistence.domain.User;
import com.hmdm.rest.filter.AuthFilter;
import com.hmdm.rest.json.AuthOptionsResponse;
import com.hmdm.rest.json.Response;
import com.hmdm.rest.json.UserCredentials;
import com.hmdm.rest.json.view.user.OIDCDetailsView;
import com.hmdm.rest.json.view.user.UserView;
import com.hmdm.service.EmailService;
import com.hmdm.service.RsaKeyService;
import com.hmdm.util.BackgroundTaskRunnerService;
import com.hmdm.util.PasswordUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * A resource for authenticating the users based on provided login/password credentials.
 *
 * @author isv
 */
@Singleton
@Path("/public/auth")
public class AuthResource {

    private static final Logger logger = LoggerFactory.getLogger(AuthResource.class);

    private UnsecureDAO userDAO;
    private CustomerDAO customerDAO;
    private UnsecureDAO settingsDAO;
    private BackgroundTaskRunnerService taskRunner;
    private boolean customerSignup;
    private EmailService emailService;
    private RsaKeyService rsaKeyService;
    private boolean transmitPassword;
    private HmdmAuthInterface authEngine;

    // Replace with your actual OIDC Provider realm/application values or load via configuration
    private String oidcJwksUrl;
    private String oidcIssuer;
    private String oidcAudience;
    private String oidcScope;
    private String oidcClientId;
    private String oidcRedirectUrl;
    private String oidcAuthorizeUrl;
    private String oidcResponseType;

    /** A constructor required by Swagger. */
    public AuthResource() {}

    /** Constructs new <code>AuthResource</code> instance. */
    @Inject
    public AuthResource(
            UnsecureDAO userDAO,
            CustomerDAO customerDAO,
            UnsecureDAO settingsDAO,
            BackgroundTaskRunnerService taskRunner,
            EmailService emailService,
            RsaKeyService rsaKeyService,
            @Named("customer.signup") boolean customerSignup,
            @Named("transmit.password") boolean transmitPassword,
            // Used for changing the class responsible with authentification
            @Named("auth.class") HmdmAuthInterface authEngine,
            @Named("oidc.jwks.url") String jwksUrl,
            @Named("oidc.issuer") String issuer,
            @Named("oidc.authorize.url") String authorizeUrl,
            @Named("oidc.audience") String audience,
            @Named("oidc.scope") String scope,
            @Named("oidc.client.id") String clientId,
            @Named("oidc.redirect.url") String redirectUrl,
            @Named("oidc.response.type") String responseType) {
        this.userDAO = userDAO;
        this.customerDAO = customerDAO;
        this.settingsDAO = settingsDAO;
        this.taskRunner = taskRunner;
        this.emailService = emailService;
        this.rsaKeyService = rsaKeyService;
        this.customerSignup = customerSignup;
        this.transmitPassword = transmitPassword;
        this.authEngine = authEngine;
        this.oidcJwksUrl = jwksUrl;
        this.oidcIssuer = issuer;
        this.oidcAuthorizeUrl = authorizeUrl;
        this.oidcAudience = audience;
        this.oidcScope = scope;
        this.oidcClientId = clientId;
        this.oidcRedirectUrl = redirectUrl;
        this.oidcResponseType = responseType;
    }

    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    /**
     * Authenticates the user based on provided credentials and responds with the user account
     * details in case of successful authentication.
     *
     * @param credentials the credentials to be used for authenticating the user to application.
     * @param req an incoming request.
     * @return a response containing the details for authenticated user.
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(UserCredentials credentials, @Context HttpServletRequest req)
            throws InterruptedException {
        if (credentials.isToken()) {
            return loginOIDC(credentials.getToken());
        }

        return loginLocal(credentials, req);
    }

    public Response loginOIDC(String token) throws InterruptedException {

        try {

            // decode jwt
            JwkProvider provider =
                    new JwkProviderBuilder(oidcJwksUrl)
                            .cached(10, 24, TimeUnit.HOURS)
                            .rateLimited(10, 1, TimeUnit.MINUTES)
                            .build();

            RSAKeyProvider keyProvider =
                    new RSAKeyProvider() {
                        @Override
                        public RSAPublicKey getPublicKeyById(String kid) {
                            try {
                                // Fetch the JWK by ID and extract the RSA Public Key
                                return (RSAPublicKey) provider.get(kid).getPublicKey();
                            } catch (Exception e) {
                                throw new RuntimeException(
                                        "Failed to retrieve public key for kid: " + kid, e);
                            }
                        }

                        @Override
                        public RSAPrivateKey getPrivateKey() {
                            return null; // Not needed for token verification
                        }

                        @Override
                        public String getPrivateKeyId() {
                            return null; // Not needed for token verification
                        }
                    };

            Algorithm algorithm = Algorithm.RSA256(keyProvider);
            JWTVerifier verifier =
                    JWT.require(algorithm)
                            .withIssuer(oidcIssuer)
                            .withAnyOfAudience(oidcAudience)
                            .build();

            DecodedJWT decodedJWT = verifier.verify(token);

            // verify existence of user

            String sub = decodedJWT.getSubject();

            User user = authEngine.findUser(sub);
            if (user == null) {
                Thread.sleep(1000);
                return Response.ERROR("Inexistent user");
            }

            user.setPassword(null);

            return Response.OK();

        } catch (JWTVerificationException exception) {
            return Response.ERROR(exception.getMessage());
        }
    }

    @GET
    @Path("/oidc-details")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOidcDetails(@Context HttpServletRequest request) {
        // Unic state (anti-CSRF)
        String state = UUID.randomUUID().toString();
        request.getSession().setAttribute("oidc_state", state);

        logger.debug(
                "[OIDC] -> clientId: '{}', authorizeUrl: '{}', redirectUrl: '{}'",
                this.oidcClientId,
                this.oidcAuthorizeUrl,
                this.oidcRedirectUrl);

        OIDCDetailsView details =
                new OIDCDetailsView(
                        this.oidcClientId,
                        this.oidcRedirectUrl,
                        this.oidcAuthorizeUrl,
                        this.oidcScope,
                        state,
                        this.oidcResponseType);

        return Response.OK(details);
    }

    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    public Response loginLocal(UserCredentials credentials, @Context HttpServletRequest req)
            throws InterruptedException {
        if (credentials.getLogin() == null || credentials.getPassword() == null) {
            return Response.ERROR();
        }

        User user = authEngine.findUser(credentials.getLogin());
        if (user == null) {
            Thread.sleep(1000);
            return Response.ERROR();
        }
        if (user.getLastLoginFail() > System.currentTimeMillis() - 1000) {
            // No delay to avoid server overload while there's a brute force attack
            Thread.sleep(1000);
            return Response.ERROR();
        }

        String password = null;
        if (transmitPassword) {
            byte[] passEnc = Base64.getDecoder().decode(credentials.getPassword());
            password = rsaKeyService.decrypt(passEnc);
        } else {
            password = credentials.getPassword();
        }

        // Web app sends MD5 hash, we need to re-hash it to compare with the DB value
        if (!authEngine.authenticate(user, password)) {
            Thread.sleep(1000);
            return Response.ERROR();
        }

        try {
            this.taskRunner.submitTask(
                    () -> {
                        this.customerDAO.recordLastLoginTime(
                                user.getCustomerId(), System.currentTimeMillis());
                    });

            HttpSession userSession = req.getSession();
            userSession.setAttribute(AuthFilter.sessionCredentials, user);

            Settings settings = settingsDAO.getSettings(user.getCustomerId());
            if (settings != null) {
                if (settings.isTwoFactor()) {
                    userSession.setAttribute(AuthFilter.twoFactorNeeded, "true");
                    user.setTwoFactor(true);
                }
                user.setIdleLogout(settings.getIdleLogout());
            }

            if (user.getAuthToken() == null || user.getAuthToken().length() == 0) {
                user.setAuthToken(PasswordUtil.generateToken());
                user.setNewPassword(
                        user.getPassword()); // copy value for setUserNewPasswordUnsecure
                userDAO.setUserNewPasswordUnsecure(user);
            }

            user.setPassword(null);

            user.setSingleCustomer(userDAO.isSingleCustomer());

            return Response.OK(new UserView(user));
        } catch (Exception e) {
            e.printStackTrace();
            return Response.INTERNAL_ERROR();
        }
    }

    /**
     * Logs the current user out by invalidating the current session.
     *
     * @param req an incoming request.
     */
    @POST
    @Path("/logout")
    public void logout(@Context HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** Returns the login options */
    @GET
    @Path("/options")
    public Response options() {
        AuthOptionsResponse response = new AuthOptionsResponse();
        response.setSignup(emailService.isConfigured() && customerSignup);
        response.setRecover(emailService.isConfigured());
        if (transmitPassword) {
            PublicKey publicKey = rsaKeyService.getPublicKey();
            byte[] keyBytes = publicKey.getEncoded();
            String encoded = Base64.getEncoder().encodeToString(keyBytes);
            response.setPublicKey(encoded);
        }
        return Response.OK(response);
    }
}
