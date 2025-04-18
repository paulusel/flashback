package org.flashback.auth;

import java.time.Instant;
import java.util.Date;

import org.flashback.helpers.Config;
import org.eclipse.jetty.http.HttpStatus;
import org.flashback.exceptions.FlashbackException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;

public class Authenticator {

    /**
     * @param exchange http request that contains JWT token
     * @return username of the user if authenticated, or null otherwise
     * @throws FlashbackException if token not found or invalid or expired
     */
    public static String authenticate(HttpServletRequest exchange) throws FlashbackException {
        try {
            String header = exchange.getHeader("Authorization");
            if(header == null || !header.startsWith("Bearer ")) {
                throw new FlashbackException("token not found in request header");
            }

            String token = header.substring(7);
            return Jwts.parser()
                .verifyWith(Config.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        }
        catch(JwtException e) {
            throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "invalid or expired bearer token");
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
