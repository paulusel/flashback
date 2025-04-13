package org.flashback.server.handlers;

import java.io.IOException;

import org.flashback.server.RequestResponsePair;


public class Handler {

    public static void sendResponse(int statusCode, String response, RequestResponsePair exchange) {
        try( var out = exchange.getResponse().getOutputStream();) {
            exchange.getResponse().setStatus(statusCode);
            out.write(response.getBytes());
        }
        catch(IOException e) {
            System.out.println("Error occured while sending response ...");
            e.printStackTrace();
        }
    }
}
