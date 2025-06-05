package org.flashback.helpers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Config {
    public static final HashMap<String, String> configValues = new HashMap<>();
    public static final List<String> mandatory_keys = Arrays.asList("dbhost", "dbname", "dbusername", "dbpassword",
        "server_port", "public_key", "private_key", "uploads_dir",  "bot_token"
    );

    public static final List<String> numeric_keys = Arrays.asList("jwt_token_expiry", "server_port", "server_nthreads",
        "server_queue_size", "max_file_size"
    );

    public static final List<String> file_keys = Arrays.asList("public_key", "private_key", "uploads_dir", "temp_dir");

    public static void init(Path configFile) throws Exception {
        try(
            FileReader reader = new FileReader(configFile.toString());
            BufferedReader lineReader = new BufferedReader(reader))
        {
            lineReader.lines().forEach(line -> {
                line = line.trim();
                if(line.isEmpty()) return;
                int indx = line.indexOf('=');
                if(indx < 1) {
                    throw new IllegalArgumentException("illegal entry in config file: [" + line + "]");
                }
                String key = line.substring(0, indx).trim();
                String value = line.substring(indx + 1).trim();
                configValues.put(key, value == null ? "" : value);
            });
        }

        mandatory_keys.stream().forEach(key -> {
            String value = configValues.get(key);
            if(value == null || value.isEmpty()) {
                throw new RuntimeException("missing " + key + " in config");
            }
        });

        numeric_keys.forEach(key -> {
            String value = configValues.get(key);
            if(value == null || value.isEmpty()) return;
            if(!StringUtils.isNumeric(value)) {
                throw new RuntimeException("expected numberic value for " + key + " in config. found: " + value);
            }
            Integer num = Integer.valueOf(value);
            if(num < 1 || (key.equals("server_port") && num > 65535) || (!key.equals("server_port") && num > 1000)) {
                throw new RuntimeException("config value for [" + key + "] in config is out of bound. value: " + value);
            }
        });

        file_keys.forEach(key -> {
            String value = configValues.get(key);
            if(value == null || value.isEmpty()) return;

            Path filePath = Path.of(value);
            if(!Files.exists(filePath)) {
                throw new RuntimeException("config value for [" + key + "]: file not found. value: " + value);
            }
        });
    }

    public static String getValue(String key) {
        return configValues.get(key);
    }
}
