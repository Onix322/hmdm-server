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

import com.hmdm.auth.AuthStrategy;
import com.hmdm.auth.local.LocalAuthCredentials;
import com.hmdm.persistence.domain.User;
import com.hmdm.rest.json.AuthOptionsResponse;
import com.hmdm.rest.json.Response;
import com.hmdm.rest.json.UserCredentials;
import com.hmdm.rest.json.view.user.UserView;
import com.hmdm.service.EmailService;
import com.hmdm.service.RsaKeyService;
import com.hmdm.util.AuthHelper;

import java.security.PublicKey;
import java.util.Base64;

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
public class LocalAuthResource {

    private AuthHelper authHelper;
    private boolean customerSignup;
    private EmailService emailService;
    private RsaKeyService rsaKeyService;
    private boolean transmitPassword;
    private AuthStrategy<LocalAuthCredentials> authEngine;

    /** A constructor required by Swagger. */
    public LocalAuthResource() {}

    /** Constructs new <code>AuthResource</code> instance. */
    @Inject
    public LocalAuthResource(
            AuthHelper authHelper,
            EmailService emailService,
            RsaKeyService rsaKeyService,
            @Named("customer.signup") boolean customerSignup,
            @Named("transmit.password") boolean transmitPassword,
            // Used for changing the class responsible with authentification
            AuthStrategy<LocalAuthCredentials> authEngine) {
        this.authHelper = authHelper;
        this.emailService = emailService;
        this.rsaKeyService = rsaKeyService;
        this.customerSignup = customerSignup;
        this.transmitPassword = transmitPassword;
        this.authEngine = authEngine;
    }

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

        LocalAuthCredentials localAuthCredentials = new LocalAuthCredentials(user, password);
        // Web app sends MD5 hash, we need to re-hash it to compare with the DB value
        if (!authEngine.authenticate(localAuthCredentials)) {
            Thread.sleep(1000);
            return Response.ERROR();
        }

        UserView userView = this.authHelper.createUserView(user, req);
        return Response.OK(userView);
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
