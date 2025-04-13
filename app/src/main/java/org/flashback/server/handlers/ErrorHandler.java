package org.flashback.server.handlers;

import org.flashback.server.RequestResponsePair;

/**
 * ServerErrorHandler
 */
public class ErrorHandler extends Handler{

    public static void handle(int errorCode, String message, RequestResponsePair exchange) {
        exchange.getResponse().setHeader("Content-Type", "application/json");
        String response = "{\"status\":\"fail\", \"message\":\"" + message + "\"}";
        Handler.sendResponse(errorCode, response, exchange);
    }
}
