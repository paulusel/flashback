package org.flashback.server.handlers;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;

import org.flashback.types.RequestResponsePair;
import org.flashback.types.ServerResponse;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MemoFile;
import org.flashback.helpers.Config;

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
        exchange.getResponse().reset();
        ServerResponse response = new ServerResponse(false, e.getStatusCode(), e.getMessage());
        sendJsonResponse(response, exchange);
    }

    public static void checkJsonBody(HttpServletRequest request) throws FlashbackException {
        String contentType = request.getHeader("Content-Type");
        if(contentType == null || !contentType.equals("application/json")) {
            throw new FlashbackException("expected JSON body");
        }
    }

    public static void sendSuccess(RequestResponsePair exchange, String message) {
        ServerResponse response = new ServerResponse(true, HttpStatus.OK_200, message);
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

    public static void sendFile(RequestResponsePair exchange, MemoFile mFile) throws IOException {
        var file = Config.getUploadsdir().resolve(mFile.getFileId()).toFile();
        var response = exchange.getResponse();
        try(FileInputStream in = new FileInputStream(file)){
            response.setStatus(HttpStatus.OK_200);
            response.setHeader("Content-Type", mFile.getMime_type());
            response.setHeader("Content-Disposition", String.format("inline; filename=\"%s\"", mFile.getOriginalName()));
            response.setHeader("Content-Length", String.valueOf(mFile.getSize()));
            in.transferTo(response.getOutputStream());
        }
    }

    public static void sendServerError(RequestResponsePair exchange) {
        ServerResponse response = new ServerResponse(false, HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal Server Error");
        sendJsonResponse(response, exchange);
    }

    public static void sendJsonExpecedError(RequestResponsePair exchange) {
        ServerResponse response = new ServerResponse(false, HttpStatus.BAD_REQUEST_400, "Expected JSON Data");
        Handler.sendJsonResponse(response, exchange);
    }
}
