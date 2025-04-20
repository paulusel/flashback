package org.flashback.server.handlers;

import java.io.IOException;

import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.Config;

import com.google.gson.Gson;

public class FileDownloadHandler {
    public static void handle(RequestResponsePair exchange) {
        try{
            String username = Authenticator.authenticate(exchange.getRequest());
            String json = Handler.requestBodyString(exchange.getRequest());
            String fileId = new Gson().fromJson(json, String.class);
            var file = Database.getFile(username, fileId);

            //check size limits
            int remaining_size =  Config.getMaxTransferableBytes(); 
            if(file.getSize() > remaining_size ) {
                throw new FlashbackException("cumulative size exceeeds allowed transfer limit");
            }

            try {
                Handler.sendFile(exchange, file);
            }
            catch(IOException e) {
                e.printStackTrace();
            }

        }
        catch(FlashbackException e) {
            Handler.handleException(exchange, e);
        }
    }
}
