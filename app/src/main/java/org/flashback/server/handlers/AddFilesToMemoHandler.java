package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.FileAddRemoveRequest;
import org.flashback.types.MessageResponse;
import org.flashback.types.RequestResponsePair;

import com.google.gson.Gson;

public class AddFilesToMemoHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            String username = Authenticator.authenticate(exchange.getRequest());
            Handler.checkJsonBody(exchange.getRequest());
            String json = Handler.requestBodyString(exchange.getRequest());
            FileAddRemoveRequest[] requests = new Gson().fromJson(json, FileAddRemoveRequest[].class);
            Database.addMemoFiles(username, requests);
            MessageResponse response = new MessageResponse(true, HttpStatus.OK_200, "added successfully");
            Handler.sendJsonResponse(response, exchange);
        }
        catch(FlashbackException e) {
            Handler.handleException(exchange, e);
        }
    }
}
