package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;

import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.types.AuthResponse;
import org.flashback.types.FlashBackUser;
import org.flashback.helpers.*;

public class LoginHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            GenericHandler.checkJsonBody(exchange.request);
            String json = GenericHandler.getRequestBodyString(exchange.request);
            FlashBackUser user = Json.deserialize(json, FlashBackUser.class);
            if(user.getUserName() == null || user.getPassword() == null) {
                throw new FlashbackException("username or password missing");
            }

            user = Database.authenticate(user);
            String token = Authenticator.generateToken(user.getUserId());
            exchange.response.addCookie(GenericHandler.makeAuthCookie(token, "token"));

            AuthResponse response = new AuthResponse(true, HttpStatus.OK_200, token, user);
            GenericHandler.sendResponse(response, exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }
}
