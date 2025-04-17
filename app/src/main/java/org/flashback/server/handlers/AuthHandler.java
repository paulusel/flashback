package org.flashback.server.handlers;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;

import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.server.RequestResponsePair;
import org.flashback.types.AuthResponse;
import org.flashback.types.MessageResponse;
import org.flashback.types.User;

import com.google.gson.Gson;

public class AuthHandler {

    public static void handle(RequestResponsePair exchange, Database db) {
        if(!exchange.getRequest().getHeader("Content-Type").equals("application/json")) {
            MessageResponse response = new MessageResponse(false, HttpStatus.BAD_REQUEST_400, "Expected JSON Data");
            Handler.sendJson(response, exchange);
            return;
        }

        try {
            String json = IOUtils.toString(exchange.getRequest().getInputStream(), StandardCharsets.UTF_8);
            User user = new Gson().fromJson(json, User.class);
            if(user.getUserName() == null || user.getPassword() == null) {
                MessageResponse response = new MessageResponse(false, HttpStatus.BAD_REQUEST_400, "Credentials missing");
                Handler.sendJson(response, exchange);
                return;
            }

            if(!db.authenticate(user)) {
                MessageResponse response = new MessageResponse(false, HttpStatus.UNAUTHORIZED_401,
                    "Incorrect username or password");
                Handler.sendJson(response, exchange);
                return;
            }

            String token = Authenticator.generateToken(user.getUserName());
            user = db.getUser(user.getUserName());
            AuthResponse response = new AuthResponse(true, HttpStatus.OK_200, token, user);
            Handler.sendJson(response, exchange);
        }
        catch(Exception e) {
            e.printStackTrace();
            Handler.sendServerError(exchange);
        }
    }
}
