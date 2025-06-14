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

public class GetFeedHandler {
    public static void handle(RequestResponsePair exchange) {
        try {
            Integer userId = Authenticator.authenticate(exchange.request);
            String json = GenericHandler.getRequestBodyString(exchange.request);
            long timestamp = Json.deserialize(json, long.class);

            List<FlashBackNote> notes = Database.getFeed(userId, timestamp);
            GenericHandler.sendResponse(new MultipleNoteResponse(true, HttpStatus.OK_200, notes), exchange);
        }
        catch(FlashbackException e)  {
            e.printStackTrace();
            GenericHandler.handleException(exchange, e);
        }
        catch(Exception e) {
            e.printStackTrace();
            GenericHandler.handleException(exchange, new FlashbackException());
        }
    }
}
