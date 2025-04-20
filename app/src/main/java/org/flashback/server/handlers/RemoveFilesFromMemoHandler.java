package org.flashback.server.handlers;

import org.flashback.types.RequestResponsePair;
import org.flashback.types.ServerResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.FileAddRemoveRequest;

import com.google.gson.Gson;

public class RemoveFilesFromMemoHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            String username = Authenticator.authenticate(exchange.getRequest());
            Handler.checkJsonBody(exchange.getRequest());
            String json = Handler.requestBodyString(exchange.getRequest());
            FileAddRemoveRequest[] requests = new Gson().fromJson(json, FileAddRemoveRequest[].class);
            Database.removeMemoFiles(username, requests);
            ServerResponse response = new ServerResponse(true, HttpStatus.OK_200, "removed successfully");
            Handler.sendJsonResponse(response, exchange);
        }
        catch(FlashbackException e) {
            Handler.handleException(exchange, e);
        }
    }
}
