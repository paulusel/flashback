package org.flashback.helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.commons.io.FilenameUtils;

import org.eclipse.jetty.http.HttpStatus;

import org.flashback.types.RequestResponsePair;
import org.flashback.types.ServerResponse;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MessageResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class GenericHandler {
    public static String getRequestBodyString(HttpServletRequest request) throws FlashbackException {
        try{
            return IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        }
        catch(IOException e){
            throw new FlashbackException();
        }
    }

    public static void handleException(RequestResponsePair exchange, FlashbackException e) {
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

        try(var out = exchange.response.getOutputStream()) {
            out.write(json.getBytes());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static Cookie makeAuthCookie(String token, String name) {
        Cookie cookie = new Cookie(name, token);
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        cookie.setPath("/");
        return cookie;
    }

    public static void sendFile(RequestResponsePair exchange, Path filePath, boolean download) throws IOException {
        try(FileInputStream in = new FileInputStream(filePath.toString())){

            String contentType = new Tika().detect(filePath);
            String fileName = FilenameUtils.getName(filePath.toString());
            Long size = Files.size(filePath);
            String disposition = String.format("%s; filename=\"%s\"", download ? "download" : "inline", fileName);

            exchange.response.setStatus(HttpStatus.OK_200);
            exchange.response.setHeader("Content-Type", contentType);
            exchange.response.setHeader("Content-Disposition", disposition);
            exchange.response.setHeader("Content-Length", String.valueOf(size));
            in.transferTo(exchange.response.getOutputStream());
        }
    }
}
