package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.types.AuthResponse;
import org.flashback.types.User;

import com.google.gson.Gson;

public class SignupHandler {
    public static void handle(RequestResponsePair exchange) {
        try {
            Handler.checkJsonBody(exchange.getRequest());
            String json = Handler.requestBodyString(exchange.getRequest());
            User user = new Gson().fromJson(json, User.class);
            Database.addNewUser(user);
            String token = Authenticator.generateToken(user.getUsername());
            AuthResponse response = new AuthResponse(true, HttpStatus.CREATED_201, token, user);
            Handler.sendJsonResponse(response, exchange);
        }
        catch(FlashbackException e) {
            Handler.handleException(exchange, e);
        }
    }
}
