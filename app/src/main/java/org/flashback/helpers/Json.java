package org.flashback.helpers;

import com.google.gson.Gson;

public class Json {
    private static Gson converter = new Gson();

    public static <T> T deserialize(String json, Class<T> type) {
        return converter.fromJson(json, type);
    }

    public static <T> String serialize(T object) {
        return converter.toJson(object);
    }
}
