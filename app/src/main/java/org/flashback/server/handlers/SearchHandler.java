package org.flashback.server.handlers;

import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.types.FlashBackNote;
import org.flashback.types.MultipleNoteResponse;
import org.flashback.helpers.*;

public class SearchHandler{
    public static void handle(RequestResponsePair exchange) {
        try {
            Integer userId = Authenticator.authenticate(exchange.request);
            GenericHandler.checkJsonBody(exchange.request);
            String json = GenericHandler.getRequestBodyString(exchange.request);
            String keyword = Json.deserialize(json, String.class);
            List<FlashBackNote> notes = Database.searchNotes(userId, keyword);
            GenericHandler.sendResponse(new MultipleNoteResponse(true, HttpStatus.OK_200, notes), exchange);
        }
        catch(FlashbackException e)  {
            GenericHandler.handleException(exchange, e);
        }
    }
}
