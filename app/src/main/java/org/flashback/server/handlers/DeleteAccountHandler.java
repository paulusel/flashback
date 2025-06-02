package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MessageResponse;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.*;

public class DeleteAccountHandler {

    public static void handle(RequestResponsePair exchange) {
        try{
            Long userId = Authenticator.authenticate(exchange.getRequest());
            Database.deleteUser(userId);
            MessageResponse response = new MessageResponse(true, HttpStatus.NO_CONTENT_204, "deleted");
            GenericHandler.sendResponse(response, exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }
}
