package org.flashback.auth;

import java.time.Instant;
import java.util.Date;
import java.util.Arrays;

import org.flashback.helpers.Config;
import org.flashback.exceptions.FlashbackException;
import org.flashback.exceptions.VerificationException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.*;

public class Authenticator {

    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private static int expiry;

    public static void init() throws Exception {
        String exp = Config.getValue("jwt_token_expiry");
        if(exp == null || exp.isEmpty()) exp = "720";
        expiry = Integer.valueOf(exp) * 3600;

        String privateKeyPath = Config.getValue("private_key");
        String publicKeyPath = Config.getValue("public_key");

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            byte[] priv = Files.readAllBytes(Paths.get(privateKeyPath));
            byte[] pub = Files.readAllBytes(Paths.get(publicKeyPath));

            PKCS8EncodedKeySpec privateKeySpec= new PKCS8EncodedKeySpec(priv);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pub);

            publicKey = keyFactory.generatePublic(publicKeySpec);
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(256);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

            Files.write(Paths.get(privateKeyPath), privateKey.getEncoded());
            Files.write(Paths.get(publicKeyPath), publicKey.getEncoded());
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
            throw new VerificationException();
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

    private static String extractCookie(HttpServletRequest request) {
        var cookies = request.getCookies();
        if(cookies == null) return "";

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals("token"))
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

    public static String generateOtpToken(Integer userId) {
        return Jwts.builder()
                .signWith(privateKey)
                .subject(String.valueOf(userId))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .compact();
    }

    public static Integer verifyOtpToken(String token) throws VerificationException {
        try {
            String userIdStr = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

            return Integer.valueOf(userIdStr);
        }
        catch(JwtException e) {
            throw new VerificationException();
        }
    }
}
