package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;

import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.types.AuthResponse;
import org.flashback.types.User;

import com.google.gson.Gson;

public class LoginHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            Handler.checkJsonBody(exchange.getRequest());
            String json = Handler.requestBodyString(exchange.getRequest());
            User user = new Gson().fromJson(json, User.class);
            if(user.getUsername() == null || user.getPassword() == null) {
                throw new FlashbackException("username or password missing");
            }

            if(!Database.authenticate(user)) {
                throw new FlashbackException(HttpStatus.UNAUTHORIZED_401, "incorrect username or password");
            }

            String token = Authenticator.generateToken(user.getUsername());
            user = Database.getUser(user.getUsername());
            AuthResponse response = new AuthResponse(true, HttpStatus.OK_200, token, user);
            Handler.sendJsonResponse(response, exchange);
        }
        catch(FlashbackException e) {
            Handler.handleException(exchange, e);
        }
    }
}
