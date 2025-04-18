package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.types.User;

import com.google.gson.Gson;

public class DeleteAccountHandler {

    public static void handle(RequestResponsePair exchange) {
        try{
            Handler.checkJsonBody(exchange.getRequest());
            String json = Handler.requestBodyString(exchange.getRequest());
            User user = new Gson().fromJson(json, User.class);
            Database.deleteUser(user);
            exchange.getResponse().setStatus(HttpStatus.NO_CONTENT_204);
        }
        catch(FlashbackException e) {
            Handler.handleException(exchange, e);
        }
    }
}
