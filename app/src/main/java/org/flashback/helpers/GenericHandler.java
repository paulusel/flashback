package org.flashback.helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;

import org.flashback.types.RequestResponsePair;
import org.flashback.types.ServerResponse;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MessageResponse;
import org.flashback.types.NoteFile;

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
        exchange.getResponse().reset();
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
        exchange.getResponse().setHeader("Content-Type", "application/json");
        exchange.getResponse().setStatus(response.getStatusCode());
        String json = Json.serialize(response);

        try( var out = exchange.getResponse().getOutputStream();) {
            out.write(json.getBytes());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendFile(RequestResponsePair exchange, NoteFile mFile) throws IOException {
        var file = Path.of(Config.getValue("uploads_dir")).resolve(mFile.getFileId()).toFile();
        var response = exchange.getResponse();
        try(FileInputStream in = new FileInputStream(file)){
            response.setStatus(HttpStatus.OK_200);
            response.setHeader("Content-Type", mFile.getMimeType());
            response.setHeader("Content-Disposition", String.format("inline; filename=\"%s\"", mFile.getFileName()));
            response.setHeader("Content-Length", String.valueOf(mFile.getSize()));
            in.transferTo(response.getOutputStream());
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
