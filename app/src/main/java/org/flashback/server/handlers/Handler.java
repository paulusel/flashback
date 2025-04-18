package org.flashback.server.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;

import org.flashback.types.RequestResponsePair;
import org.flashback.types.ServerResponse;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MessageResponse;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;


public class Handler {

    public static String requestBodyString(HttpServletRequest request) throws FlashbackException {
            try{
                return IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
            }
            catch(IOException e){
                throw new FlashbackException("");
            }
    }

    public static void handleException(RequestResponsePair exchange, FlashbackException e) {
        MessageResponse response = new MessageResponse(false, e.getStatusCode(), e.getMessage());
        sendJsonResponse(response, exchange);
    }

    public static void checkJsonBody(HttpServletRequest request) throws FlashbackException {
        String contentType = request.getHeader("Content-Type");
        if(contentType == null || !contentType.equals("application/json")) {
            throw new FlashbackException("expected JSON body");
        }
    }

    public static void sendSuccess(RequestResponsePair exchange, String message) {
        MessageResponse response = new MessageResponse(true, HttpStatus.OK_200, message);
        sendJsonResponse(response, exchange);
    }

    public static void sendResponse(int statusCode, String response, RequestResponsePair exchange) {
        try( var out = exchange.getResponse().getOutputStream();) {
            exchange.getResponse().setStatus(statusCode);
            out.write(response.getBytes());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendJsonResponse(ServerResponse response, RequestResponsePair exchange) {
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
        sendJsonResponse(response, exchange);
    }

    public static void sendJsonExpecedError(RequestResponsePair exchange) {
        MessageResponse response = new MessageResponse(false, HttpStatus.BAD_REQUEST_400, "Expected JSON Data");
        Handler.sendJsonResponse(response, exchange);
    }
}
