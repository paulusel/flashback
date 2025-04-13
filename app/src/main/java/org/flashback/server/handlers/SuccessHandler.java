package org.flashback.server.handlers;

import java.net.HttpURLConnection;

import org.flashback.server.RequestResponsePair;

/**
 * SuccessHandler
 */
public class SuccessHandler {

    public static void handle(String message, RequestResponsePair exchange) {
        exchange.getResponse().setHeader("Content-Type", "application/json");
        String response = "{\"status\":\"ok\", \"message\":\"" + message + "\"}";
        Handler.sendResponse(HttpURLConnection.HTTP_OK, response, exchange);
    }
}
