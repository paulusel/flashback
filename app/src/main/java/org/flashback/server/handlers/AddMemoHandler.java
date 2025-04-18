package org.flashback.server.handlers;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.ListResponse;
import org.flashback.types.MemoItem;
import org.flashback.types.RequestResponsePair;

import com.google.gson.Gson;

public class AddMemoHandler {
    public static void handle(RequestResponsePair exchange) {
        try {
            String username = Authenticator.authenticate(exchange.getRequest());
            Handler.checkJsonBody(exchange.getRequest());
            String json = Handler.requestBodyString(exchange.getRequest());
            MemoItem[] memoItems = new Gson().fromJson(json, MemoItem[].class);
            var list = handle(username, memoItems);
            ListResponse<MemoItem> response = new ListResponse<>(true, HttpStatus.OK_200, list);
            Handler.sendJsonResponse(response, exchange);
        }
        catch(FlashbackException e) {
            Handler.handleException(exchange, e);
        }
    }

    public static List<MemoItem> handle(String username, MemoItem[] memoItems) throws FlashbackException {
        for(var item : memoItems) {
            if(item.getFiles() == null) continue;
            var uniqueFiles = item.getFiles().stream().distinct().collect(Collectors.toList());
            item.setFiles(uniqueFiles);
        }

        return Database.addMemo(username, memoItems);
    }
}
