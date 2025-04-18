package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.types.ListResponse;
import org.flashback.types.MemoItem;

import com.google.gson.Gson;

public class GetMemoHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            String username = Authenticator.authenticate(exchange.getRequest());
            Handler.checkJsonBody(exchange.getRequest());
            String json = Handler.requestBodyString(exchange.getRequest());
            int[] item_ids = new Gson().fromJson(json, int[].class);
            var list = Database.getMemoItems(username, item_ids);
            ListResponse<MemoItem> response = new ListResponse<>(true, HttpStatus.OK_200, list);
            Handler.sendJsonResponse(response, exchange);
        }
        catch(FlashbackException e)  {
            Handler.handleException(exchange, e);
        }
    }
}
