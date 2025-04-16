package org.flashback.helpers;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import io.jsonwebtoken.Jwts;

public class Config {
    private static final KeyPair pair;
    private static int EXPIRATION_SECONDS = 30 * 24 * 3600;
    private static final String databaseAddress = "jdbc:postgresql://localhost/";
    private final static String dbName = "flashback";
    private final static String dbUserName = "flashback";
    private final static String dbPassword = "";

    static {
        pair = Jwts.SIG.EdDSA.keyPair().build();
    }

    public static PublicKey getPublicKey() {
        return pair.getPublic();
    }

    public static PrivateKey getPrivateKey() {
        return pair.getPrivate();
    }

    public static int getExpirationDuration() {
        return EXPIRATION_SECONDS;
    }

    public static String getDatabaseName() {
        return dbName;
    }

    public static String getDatabaseUrl() {
        return databaseAddress + dbName;
    }

    public static String getDatabaseUserName() {
        return dbUserName;
    }

    public static String getDbpassword() {
        return dbPassword;
    }
}
