package org.flashback.server.handlers;

import org.flashback.types.FlashBackUser;
import org.flashback.types.UserDataResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MessageResponse;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.*;

public class GetMeHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            Integer userId = Authenticator.authenticate(exchange.request);
            FlashBackUser me = Database.getUser(userId);

            if(me == null) {
                GenericHandler.sendResponse(
                    new MessageResponse(false, HttpStatus.NOT_FOUND_404, "User Not Found"),
                    exchange);
                return;
            }

            UserDataResponse response = new UserDataResponse(true, HttpStatus.OK_200, me);
            GenericHandler.sendResponse(response, exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }
}
