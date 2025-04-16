package org.flashback.auth;

import java.time.Instant;
import java.util.Date;

import org.flashback.helpers.Config;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;

public class Authenticator {

    /**
     * @param exchange http request that contains JWT token
     * @return username of the user if authenticated, or null otherwise
     */
    public static String authenticate(HttpServletRequest exchange) {
        String header = exchange.getHeader("Authorization");
        if(header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7);
        try {
            return Jwts.parser()
                .verifyWith(Config.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param username Username to be the subject of the token
     * @return token generated
     */

    public static String generateToken(String username) {
        return Jwts.builder()
            .signWith(Config.getPrivateKey())
            .subject(username)
            .expiration(Date.from(Instant.now().plusSeconds(Config.getExpirationDuration())))
            .compact();
    }
}
