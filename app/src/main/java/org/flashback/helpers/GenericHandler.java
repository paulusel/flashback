package org.flashback.helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.eclipse.jetty.http.HttpStatus;

import org.flashback.types.RequestResponsePair;
import org.flashback.types.ServerResponse;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MessageResponse;
import org.flashback.types.FlashBackFile;

import jakarta.servlet.http.HttpServletRequest;


public class GenericHandler {

    public static String requestBodyString(HttpServletRequest request) throws FlashbackException {
        try{
            return IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        }
        catch(IOException e){
            throw new FlashbackException();
        }
    }

    public static void handleException(RequestResponsePair exchange, FlashbackException e) {
        exchange.response.reset();
        ServerResponse response = new MessageResponse(false, e.getStatusCode(), e.getMessage());
        sendResponse(response, exchange);
    }

    public static void checkJsonBody(HttpServletRequest request) throws FlashbackException {
        String contentType = request.getHeader("Content-Type");
        if(contentType == null || !contentType.equals("application/json")) {
            throw new FlashbackException("expected JSON body");
        }
    }

    public static void sendResponse(ServerResponse response, RequestResponsePair exchange) {
        exchange.response.setHeader("Content-Type", "application/json");
        exchange.response.setStatus(response.getStatusCode());
        String json = Json.serialize(response);

        try( var out = exchange.response.getOutputStream();) {
            out.write(json.getBytes());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendFile(RequestResponsePair exchange, FlashBackFile nFile) throws IOException {
        var file = Path.of(Config.getValue("uploads_dir")).resolve(nFile.getHash()).toFile();
        try(FileInputStream in = new FileInputStream(file)){
            exchange.response.setStatus(HttpStatus.OK_200);
            String contentType = new Tika().detect(nFile.getFileName());
            exchange.response.setHeader("Content-Type", contentType);
            exchange.response.setHeader("Content-Disposition", String.format("inline; filename=\"%s\"", nFile.getFileName()));
            exchange.response.setHeader("Content-Length", String.valueOf(nFile.getSize()));
            in.transferTo(exchange.response.getOutputStream());
        }
    }

    public static void sendServerError(RequestResponsePair exchange) {
        ServerResponse response = new MessageResponse(false, HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal Server Error");
        sendResponse(response, exchange);
    }

    public static void sendJsonExpecedError(RequestResponsePair exchange) {
        ServerResponse response = new MessageResponse(false, HttpStatus.BAD_REQUEST_400, "Expected JSON Data");
        GenericHandler.sendResponse(response, exchange);
    }
}
