package org.flashback.server.handlers;

import org.flashback.types.User;
import org.flashback.types.UserDataResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.types.ServerResponse;

public class GetMeHandler extends Handler{

    static void handle(RequestResponsePair exchange) {
        try {
            String username = Authenticator.authenticate(exchange.getRequest());
            if(username == null) {
                ServerResponse response = new ServerResponse(false, HttpStatus.UNAUTHORIZED_401, "Authentication Failed");
                Handler.sendJsonResponse(response, exchange);
                return;
            }

            User me = Database.getUser(username);

            if(me == null) {
                ServerResponse response = new ServerResponse(false, HttpStatus.NOT_FOUND_404, "User Not Found");
                Handler.sendJsonResponse(response, exchange);
                return;
            }

            UserDataResponse response = new UserDataResponse(true, HttpStatus.OK_200, me);
            Handler.sendJsonResponse(response, exchange);
        }
        catch(FlashbackException e) {
            Handler.handleException(exchange, e);
        }
    }
}
