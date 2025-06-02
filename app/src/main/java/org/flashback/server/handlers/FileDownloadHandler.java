package org.flashback.server.handlers;

import java.io.IOException;

import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.*;

public class FileDownloadHandler {
    public static void handle(RequestResponsePair exchange) {
        try{
            Integer userId = Authenticator.authenticate(exchange.getRequest());
            String json = GenericHandler.requestBodyString(exchange.getRequest());
            String fileId = Json.deserialize(json, String.class);
            var file = Database.getFile(userId, fileId);

            //check size limits

            try {
                GenericHandler.sendFile(exchange, file);
            }
            catch(IOException e) {
                e.printStackTrace();
            }

        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }
}
