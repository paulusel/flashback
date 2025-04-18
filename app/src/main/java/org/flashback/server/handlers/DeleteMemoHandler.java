package org.flashback.server.handlers;

import org.flashback.auth.Authenticator;
import org.flashback.exceptions.FlashbackException;
import org.flashback.database.Database;
import org.flashback.types.RequestResponsePair;

import com.google.gson.Gson;


public class DeleteMemoHandler {
    public static void handle(RequestResponsePair exchange) {
        try {
            Handler.checkJsonBody(exchange.getRequest());
            String username = Authenticator.authenticate(exchange.getRequest());
            String json = Handler.requestBodyString(exchange.getRequest());
            int[] item_ids = new Gson().fromJson(json, int[].class);
            Database.deleteMemoItems(username, item_ids);
            Handler.sendSuccess(exchange, "items deleted");
        }
        catch(FlashbackException e) {
            Handler.handleException(exchange, e);
        }
    }

}
