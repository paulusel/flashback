package org.flashback.server.handlers;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jetty.http.HttpStatus;

import org.flashback.server.RequestResponsePair;
import org.flashback.types.ServerResponse;
import org.flashback.types.MessageResponse;

import com.google.gson.Gson;


public class Handler {

    public static void sendResponse(int statusCode, String response, RequestResponsePair exchange) {
        try( var out = exchange.getResponse().getOutputStream();) {
            exchange.getResponse().setStatus(statusCode);
            out.write(response.getBytes());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendJson(ServerResponse response, RequestResponsePair exchange) {
        exchange.getResponse().setHeader("Content-Type", "application/json");
        exchange.getResponse().setStatus(response.getStatusCode());
        String json = new Gson().toJson(response);

        try( var out = exchange.getResponse().getOutputStream();) {
            out.write(json.getBytes());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendData(RequestResponsePair exchange, String mimeType, InputStream in) throws IOException {
        exchange.getResponse().setStatus(HttpStatus.OK_200);
        exchange.getResponse().setHeader("Content-Type", mimeType);
        in.transferTo(exchange.getResponse().getOutputStream());
    }

    public static void sendServerError(RequestResponsePair exchange) {
        MessageResponse response = new MessageResponse(false, HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal Server Error");
        sendJson(response, exchange);
    }
}
