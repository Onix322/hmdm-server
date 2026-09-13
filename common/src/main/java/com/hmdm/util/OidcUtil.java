package com.hmdm.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/** PkceUtil */
public class OidcUtil {

    public static String generateCodeVerifier() {
        byte[] codeVerifierBytes = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(codeVerifierBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierBytes);
    }

    public static String generateCodeChallenge(String codeVerifier)
            throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    public static String generateState() {
        return UUID.randomUUID().toString();
    }

    /** Metodă utilitară privată pentru a face URL Encode rapid pe valori */
    private static String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Metoda A: Construiește URL-ul complet de Login (Redirect) pentru faza de GET. Automat aplică
     * URL Encode pe toți parametrii sensibili.
     */
    public static String buildAuthorizeUrl(
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
     * Metoda B: Construiește CORPUL (Body-ul) cererii de Exchange (POST). NU conține URL-ul de
     * bază, ci doar parametrii form-urlencoded curați.
     */
    public static String buildRequestBody(
            String code, String redirectUri, String clientId, String codeVerifier) {
        return new StringBuilder()
                .append("grant_type=authorization_code") // Obligatoriu prin protocol!
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
