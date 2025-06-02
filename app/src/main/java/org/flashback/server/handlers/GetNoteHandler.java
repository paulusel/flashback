package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.types.FlashBackNote;
import org.flashback.types.NoteResponse;
import org.flashback.helpers.*;

public class GetNoteHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            Integer userId = Authenticator.authenticate(exchange.getRequest());
            GenericHandler.checkJsonBody(exchange.getRequest());
            String json = GenericHandler.requestBodyString(exchange.getRequest());
            int noteId = Json.deserialize(json, int.class);
            FlashBackNote note = Database.getNote(userId, noteId);
            GenericHandler.sendResponse(new NoteResponse(true, HttpStatus.OK_200, note), exchange);
        }
        catch(FlashbackException e)  {
            GenericHandler.handleException(exchange, e);
        }
    }
}
