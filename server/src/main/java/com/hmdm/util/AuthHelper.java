package com.hmdm.util;

import com.hmdm.persistence.CustomerDAO;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Settings;
import com.hmdm.persistence.domain.User;
import com.hmdm.rest.filter.AuthFilter;
import com.hmdm.rest.json.view.user.UserView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Helper class providing reusable methods for authentication and user session configuration. */
@Singleton
public class AuthHelper {

    private final UnsecureDAO userDAO;
    private final CustomerDAO customerDAO;
    private final BackgroundTaskRunnerService taskRunner;

    @Inject
    private AuthHelper(
            UnsecureDAO unsecureDAO,
            BackgroundTaskRunnerService taskRunner,
            CustomerDAO customerDAO) {
        this.taskRunner = taskRunner;
        this.userDAO = unsecureDAO;
        this.customerDAO = customerDAO;
    }

    /**
     * Configures the user session, applies customer settings, and returns a sanitized UserView.
     *
     * @param user the authenticated {@link User} entity to be configured
     * @param req the active {@link HttpServletRequest} used to retrieve or create the {@link
     *     HttpSession}
     * @return a sanitized {@link UserView} instance formatted for the frontend JSON response
     */
    public UserView createUserView(User user, HttpServletRequest req) {
        // 1. Asynchronously record last login timestamp
        this.taskRunner.submitTask(
                () -> {
                    this.customerDAO.recordLastLoginTime(
                            user.getCustomerId(), System.currentTimeMillis());
                });

        // 2. Set HTTP session attribute
        HttpSession userSession = req.getSession(true);
        userSession.setAttribute(AuthFilter.sessionCredentials, user);

        // 3. Retrieve and apply customer settings (2FA, idle logout)
        Settings settings = userDAO.getSettings(user.getCustomerId());
        if (settings != null) {
            if (settings.isTwoFactor()) {
                userSession.setAttribute(AuthFilter.twoFactorNeeded, "true");
                user.setTwoFactor(true);
            }
            user.setIdleLogout(settings.getIdleLogout());
        }

        // 4. Generate auth token if missing
        if (user.getAuthToken() == null || user.getAuthToken().length() == 0) {
            user.setAuthToken(PasswordUtil.generateToken());
            user.setNewPassword(user.getPassword());
            userDAO.setUserNewPasswordUnsecure(user);
        }

        // 5. Sanitize sensitive data before sending to client and return UserView
        user.setPassword(null);
        user.setSingleCustomer(userDAO.isSingleCustomer());

        return new UserView(user);
    }

    /**
     * Generates a secure, unpredictable PKCE Code Verifier string.
     *
     * @return a random Base64URL-encoded string used as the PKCE verifier
     */
    public String generateCodeVerifier() {
        byte[] codeVerifierBytes = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(codeVerifierBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierBytes);
    }

    /**
     * Generates a PKCE Code Challenge derived from the given verifier using SHA-256 hashing.
     *
     * @param codeVerifier the plain PKCE code verifier string
     * @return the Base64URL-encoded SHA-256 hash of the code verifier
     * @throws NoSuchAlgorithmException if the SHA-256 algorithm is not available in the environment
     */
    public String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /**
     * Required state parameter for OIDC flows to prevent CSRF attacks.
     *
     * @return a randomly generated UUID string
     */
    public String generateState() {
        return UUID.randomUUID().toString();
    }

    /** Private utility method for quick URL encoding of values */
    private String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Builds the complete Login (Redirect) URL for the initial GET phase. Automatically applies URL
     * encoding to all dynamic parameter values.
     */
    public String buildAuthorizeUrl(
            String authorizeBaseUrl,
            String clientId,
            String responseType,
            String redirectUri,
            String scope,
            String state,
            String codeChallenge) {
        return new StringBuilder()
                .append(authorizeBaseUrl)
                .append("?client_id=")
                .append(encode(clientId))
                .append("&response_type=")
                .append(encode(responseType))
                .append("&redirect_uri=")
                .append(encode(redirectUri))
                .append("&scope=")
                .append(encode(scope))
                .append("&state=")
                .append(encode(state))
                .append("&code_challenge=")
                .append(encode(codeChallenge))
                .append("&code_challenge_method=S256")
                .toString();
    }

    /**
     * Builds the request BODY for the token exchange POST request. Contains only the clean
     * form-urlencoded key-value pairs without the base URL.
     */
    public String buildRequestBody(
            String code, String redirectUri, String clientId, String codeVerifier) {
        return new StringBuilder()
                .append("grant_type=authorization_code")
                .append("&code=")
                .append(encode(code))
                .append("&redirect_uri=")
                .append(encode(redirectUri))
                .append("&client_id=")
                .append(encode(clientId))
                .append("&code_verifier=")
                .append(encode(codeVerifier))
                .toString();
    }
}
