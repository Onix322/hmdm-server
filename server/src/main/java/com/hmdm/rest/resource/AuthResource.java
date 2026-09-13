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

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdm.auth.HmdmAuthInterface;
import com.hmdm.persistence.CustomerDAO;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Settings;
import com.hmdm.persistence.domain.User;
import com.hmdm.rest.filter.AuthFilter;
import com.hmdm.rest.json.AuthOptionsResponse;
import com.hmdm.rest.json.Response;
import com.hmdm.rest.json.UserCredentials;
import com.hmdm.rest.json.view.user.UserView;
import com.hmdm.service.EmailService;
import com.hmdm.service.RsaKeyService;
import com.hmdm.util.BackgroundTaskRunnerService;
import com.hmdm.util.OidcUtil;
import com.hmdm.util.PasswordUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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
    private String oidcTokenUrl;
    private String oidcSecret;
    private Boolean oidcPkce;

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
            @Named("oidc.response.type") String responseType,
            @Named("oidc.token.url") String tokenUrl,
            @Named("oidc.secret") String secret,
            @Named("oidc.pkce") String pkce) {
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
        this.oidcTokenUrl = tokenUrl;
        this.oidcPkce = Boolean.parseBoolean(pkce);
        this.oidcSecret = secret;
    }

    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    ////////////////////////////////////////////////

    @GET
    @Path("/login-oidc")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loginOidc(@Context HttpServletRequest request)
            throws InterruptedException, NoSuchAlgorithmException {
        // 1. gets the request

        // 2. generate state and code_challager
        String state = OidcUtil.generateState();

        // 2.1 generate code_verifier and save it in session
        String codeVerifier = OidcUtil.generateCodeVerifier();

        // 2.2 generate code_challange
        String codeChallange = OidcUtil.generateCodeChallenge(codeVerifier);

        // 2.3 save in session
        HttpSession session = request.getSession(true);
        session.setAttribute("OIDC_VERIFIER", codeVerifier);
        session.setAttribute("OIDC_STATE", state);

        // 3. create redirectUrl
        String redirectUrl =
                OidcUtil.buildAuthorizeUrl(
                        this.oidcAuthorizeUrl,
                        this.oidcClientId,
                        this.oidcResponseType,
                        this.oidcRedirectUrl,
                        this.oidcScope,
                        state,
                        codeChallange);

        // 4. send the url to frontend
        Map<String, Object> innerData = new HashMap<>();
        innerData.put("redirectUrl", redirectUrl);

        return Response.OK(innerData);
    }

    @POST
    @Path("/callback-oidc")
    @Produces(MediaType.APPLICATION_JSON)
    public Response callbackOidc(
            @Context HttpServletRequest request,
            @QueryParam("code") String code,
            @QueryParam("state") String state)
            throws InterruptedException, IOException {

        // rapid pre-reqesite
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Response.ERROR("Session expired or invalid");
        }

        String oidcState = (String) session.getAttribute("OIDC_STATE");
        String oidcVerifier = (String) session.getAttribute("OIDC_VERIFIER");

        // verificare sa vedem daca state este acelasi cu cel primit
        if (state == null || !state.equals(oidcState) || oidcVerifier == null) {
            session.invalidate();
            return Response.ERROR("Session is compromised");
        }

        // creare url
        String requestBody =
                OidcUtil.buildRequestBody(
                        code, this.oidcRedirectUrl, this.oidcClientId, oidcVerifier);

        // exchange for access / id token
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest exchangeRequest =
                HttpRequest.newBuilder()
                        .uri(URI.create(this.oidcTokenUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

        HttpResponse<String> providerResponse =
                client.send(exchangeRequest, HttpResponse.BodyHandlers.ofString());

        if (providerResponse.statusCode() != 200) {
            return Response.ERROR(
                    "Provider rejected token exchange. Error: " + providerResponse.body());
        }

        String rawJson = providerResponse.body();

        // 6. delete temporary keys
        session.removeAttribute("OIDC_STATE");
        session.removeAttribute("OIDC_VERIFIER");

        // 7. Aici urmează pasul unde vei parsa rawJson ca să scoți datele userului în UserView
        // UserView userView = OidcUtil.parseIdTokenToUserView(rawJson);
        // session.setAttribute("CURRENT_USER", userView);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(rawJson);
        String idTokenString = jsonNode.path("id_token").asText();
        DecodedJWT jwt = JWT.decode(idTokenString);

        // 3. Extragere directă a claim-urilor din id_token
        String email = jwt.getClaim("email").asString();

        if (email == null || email.trim().isEmpty()) {
            return Response.ERROR("No email claim present in ID token");
        }

        // 4. Mapezi datele în obiectul tău UserView / User
        User user = this.authEngine.findUser(email);

        if (user == null) {
            return Response.ERROR("No user found");
        }

        // Pentru moment returnăm succes cu JSON-ul brut ca să vezi ce a venit de la provider
        return createUserView(user, request);
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

        return createUserView(user, req);
    }

    public Response createUserView(User user, HttpServletRequest req) {
        try {
            this.taskRunner.submitTask(
                    () -> {
                        this.customerDAO.recordLastLoginTime(
                                user.getCustomerId(), System.currentTimeMillis());
                    });

            HttpSession userSession = req.getSession(true);
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
                user.setNewPassword(user.getPassword());
                userDAO.setUserNewPasswordUnsecure(user);
            }

            user.setPassword(null);

            // Aici se setează corect proprietatea singleCustomer
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
