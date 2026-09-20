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

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdm.auth.AuthStrategy;
import com.hmdm.auth.oidc.OidcAuthCredentials;
import com.hmdm.persistence.domain.User;
import com.hmdm.rest.json.Response;
import com.hmdm.rest.json.view.user.UserView;
import com.hmdm.util.AuthHelper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
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

/** A resource for authenticating the users based on oAuth2 / OIDC protocol . */
@Singleton
@Path("/public/auth")
public class OidcAuthResource {

    private AuthStrategy<OidcAuthCredentials> authEngine;
    private AuthHelper authHelper;

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

    /** A constructor required by Swagger. */
    public OidcAuthResource() {}

    @Inject
    public OidcAuthResource(
            AuthHelper authHelper,
            AuthStrategy<OidcAuthCredentials> authEngine,
            @Named("oidc.jwks.url") String jwksUrl,
            @Named("oidc.issuer") String issuer,
            @Named("oidc.authorize.url") String authorizeUrl,
            @Named("oidc.audience") String audience,
            @Named("oidc.scope") String scope,
            @Named("oidc.client.id") String clientId,
            @Named("oidc.redirect.url") String redirectUrl,
            @Named("oidc.response.type") String responseType,
            @Named("oidc.token.url") String tokenUrl) {
        this.authHelper = authHelper;
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
    }

    @GET
    @Path("/login-oidc")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loginOidc(@Context HttpServletRequest request)
            throws InterruptedException, NoSuchAlgorithmException {
        // 1. gets the request

        // 2. generate state and code_challager
        String state = this.authHelper.generateState();

        // 2.1 generate code_verifier and save it in session
        String codeVerifier = this.authHelper.generateCodeVerifier();

        // 2.2 generate code_challange
        String codeChallange = this.authHelper.generateCodeChallenge(codeVerifier);

        // 2.3 save in session
        HttpSession session = request.getSession(true);
        session.setAttribute("OIDC_VERIFIER", codeVerifier);
        session.setAttribute("OIDC_STATE", state);

        // 3. create redirectUrl
        String redirectUrl =
                this.authHelper.buildAuthorizeUrl(
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

        // 1. prepare
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Response.ERROR("Session expired or invalid");
        }

        String oidcState = (String) session.getAttribute("OIDC_STATE");
        String oidcVerifier = (String) session.getAttribute("OIDC_VERIFIER");

        // 2. state verification
        if (state == null || !state.equals(oidcState) || oidcVerifier == null) {
            session.invalidate();
            return Response.ERROR("Session is compromised");
        }

        // 3. creare url
        String requestBody =
                this.authHelper.buildRequestBody(
                        code, this.oidcRedirectUrl, this.oidcClientId, oidcVerifier);

        // 4. exchange for access / id token
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

        // 5. delete temporary keys
        session.removeAttribute("OIDC_STATE");
        session.removeAttribute("OIDC_VERIFIER");

        // 6. decoding and extracting email (for login) from jwt
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(rawJson);
        String idTokenString = jsonNode.path("id_token").asText();

        // 6.1 cryptographic verification
        DecodedJWT jwt;
        try {
            jwt =
                    this.authHelper.verifyToken(
                            idTokenString, this.oidcJwksUrl, this.oidcIssuer, this.oidcAudience);
        } catch (Exception e) {
            return Response.ERROR("Token verification failed: " + e.getMessage());
        }

        // 6.2 extract user login (email)
        String email = jwt.getClaim("email").asString();
        if (email == null || email.trim().isEmpty()) {
            return Response.ERROR("No email claim present in ID token");
        }

        // 7. finding user in database
        User user = this.authEngine.findUser(email);
        if (user == null) {
            Thread.sleep(1000);
            return Response.ERROR("No user found");
        }

        if (user.getLastLoginFail() > System.currentTimeMillis() - 1000) {
            // No delay to avoid server overload while there's a brute force attack
            Thread.sleep(1000);
            return Response.ERROR();
        }

        OidcAuthCredentials credentials = new OidcAuthCredentials(user, idTokenString);

        if (!authEngine.authenticate(credentials)) {
            Thread.sleep(1000);
            return Response.ERROR();
        }

        UserView userView = this.authHelper.createUserView(user, request);
        return Response.OK(userView);
    }
}
