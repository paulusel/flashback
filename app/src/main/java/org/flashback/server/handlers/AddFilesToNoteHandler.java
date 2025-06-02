package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.FileAddRemoveRequest;
import org.flashback.types.MessageResponse;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.Json;
import org.flashback.helpers.GenericHandler;

public class AddFilesToNoteHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            Long userId = Authenticator.authenticate(exchange.getRequest());
            GenericHandler.checkJsonBody(exchange.getRequest());
            String json = GenericHandler.requestBodyString(exchange.getRequest());
            FileAddRemoveRequest[] requests = Json.deserialize(json, FileAddRemoveRequest[].class);
            Database.addNoteFiles(userId, requests);
            MessageResponse response = new MessageResponse(true, HttpStatus.OK_200, "added successfully");
            GenericHandler.sendResponse(response, exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }
}
