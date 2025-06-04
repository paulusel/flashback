package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.exceptions.FlashbackException;
import org.flashback.database.Database;
import org.flashback.types.MessageResponse;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.*;

public class DeleteNoteHandler {
    public static void handle(RequestResponsePair exchange) {
        try {
            GenericHandler.checkJsonBody(exchange.request);
            Integer userId = Authenticator.authenticate(exchange.request);
            String json = GenericHandler.requestBodyString(exchange.request);
            int noteId = Json.deserialize(json, int.class);
            Database.deleteNote(userId, noteId);
            MessageResponse response = new MessageResponse(true, HttpStatus.OK_200, "deleted");
            GenericHandler.sendResponse(response, exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }

}
