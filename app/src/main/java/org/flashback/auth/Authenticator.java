package org.flashback.auth;

import java.time.Instant;
import java.util.Date;
import java.util.Arrays;

import org.flashback.helpers.Config;
import org.eclipse.jetty.http.HttpStatus;
import org.flashback.exceptions.FlashbackException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

import java.security.KeyFactory;
import java.io.FileInputStream;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Authenticator {
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private static int expiry;
    public static void loadConfig() throws Exception {
        String exp = Config.getValue("jwt_token_expiry");
        if(exp == null || exp.isEmpty()) exp = "720";
        expiry = Integer.valueOf(exp) * 3600;

        try(
            FileInputStream sec  = new FileInputStream(Config.getValue("public_key"));
            FileInputStream pub = new FileInputStream(Config.getValue("private_key"));
        ) {
            var sec_bytes = sec.readAllBytes();
            var pub_bytes = pub.readAllBytes();
            KeyFactory keefactory = KeyFactory.getInstance("EdDSA");
            privateKey = keefactory.generatePrivate(new PKCS8EncodedKeySpec(sec_bytes));
            publicKey = keefactory.generatePublic(new X509EncodedKeySpec(pub_bytes));
        }
    }

    /**
     * @param request http request that contains JWT token
     * @return username of the user if authenticated, or null otherwise
     * @throws FlashbackException if token not found or invalid or expired
     */
    public static Integer authenticate(HttpServletRequest request) throws FlashbackException {
        try {
            String cookie = extractCookie(request);
            String token = cookie.isEmpty() ? extractBearer(request) : cookie;

            if(token.isEmpty()) {
                throw new FlashbackException("no authorization token found");
            }

            String userIdStr = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

            return Integer.valueOf(userIdStr);
        }
        catch(JwtException e) {
            throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "invalid or expired bearer token");
        }
    }

    /**
     * @param userId User id to be the subject of the token
     * @return token generated
     */

    public static String generateToken(Integer userId) {
        return Jwts.builder()
                .signWith(privateKey)
                .subject(String.valueOf(userId))
                .expiration(Date.from(Instant.now().plusSeconds(expiry)))
                .compact();
    }

    public static String generateOtpToken(Integer userId) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .encryptWith(publicKey, Jwts.KEY.RSA_OAEP, Jwts.ENC.A256GCM)
            .compact();
    }

    public static Integer verifyOtpToken(String token) throws FlashbackException {
        try {
            String userIdStr = Jwts.parser()
                    .decryptWith(privateKey)
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .getSubject();

            return Integer.valueOf(userIdStr);
        }
        catch(JwtException e) {
            throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "invalid or expired otp token");
        }
    }

    private static String extractCookie(HttpServletRequest request) {
        var cookies = request.getCookies();
        if(cookies == null) return "";

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals("auth_token"))
                .findFirst()
                .map(Cookie::getValue)
                .orElse("");
    }

    private static String extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if(header == null || !header.startsWith("Bearer ")) {
            return "";
        }

        return header.substring(7);
    }
}
