package org.flashback.helpers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;


public class Config {
    private static final Path configFile = Path.of("config.ini");
    private final static Path uploadsDir = Path.of("uploads");
    private static final String databaseAddress = "jdbc:postgresql://";
    public static final HashMap<String, String> configValues = new HashMap<>();
    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    private static int EXPIRATION_SECONDS = 30 * 24 * 3600;
    private static int maxTransferableBytes = 20*1024*1024;
    private static final String dbHost = "localhost";
    private final static String dbName = "flashback";
    private final static String dbUserName = "flashback";
    private final static String dbPassword = "";

    public static void configure() throws Exception {
       try(
            FileInputStream sec  = new FileInputStream("secret_key.asc");
            FileInputStream pub = new FileInputStream("public_key.asc");
        ) {
            var sec_bytes = sec.readAllBytes();
            var pub_bytes = pub.readAllBytes();
            KeyFactory keefactory = KeyFactory.getInstance("EdDSA");
            privateKey = keefactory.generatePrivate(new PKCS8EncodedKeySpec(sec_bytes));
            publicKey = keefactory.generatePublic(new X509EncodedKeySpec(pub_bytes));
        }

        try(
            FileReader reader = new FileReader(configFile.toString());
            BufferedReader lineReader = new BufferedReader(reader);)
        {
            lineReader.lines().forEach(line -> {
                line = line.trim();
                if(line.isEmpty()) return;
                int indx = line.indexOf('=');
                if(indx == -1 || indx == 0) {
                    throw new IllegalArgumentException("illegal entry in config file: [" + line + "]");
                }
                String key = line.substring(0, indx).trim();
                String value = line.substring(indx + 1).trim();
                if(key.isEmpty()) {
                    throw new IllegalArgumentException("illegal entry in config file: [" + line + "]");
                }
                configValues.put(key, value == null ? "" : value);
            });
        }
    }

    public static PublicKey getPublicKey() {
        return publicKey;
    }

    public static PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static int getExpirationDuration() {
        return EXPIRATION_SECONDS;
    }

    public static String getDatabaseName() {
        return dbName;
    }

    public static String getDatabaseUrl() {
        return databaseAddress + dbHost + "/" + dbName;
    }

    public static String getDatabaseUserName() {
        return dbUserName;
    }

    public static String getDbpassword() {
        return dbPassword;
    }

    public static Path getUploadsdir() {
        return uploadsDir;
    }

    public static int getMaxTransferableBytes() {
        return maxTransferableBytes;
    }
}
