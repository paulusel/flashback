package org.flashback.server.handlers;

import org.flashback.types.MessageResponse;
import org.flashback.types.User;
import org.flashback.types.UserDataResponse;

import java.sql.SQLException;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.server.RequestResponsePair;

import com.google.gson.Gson;

/**
 * GetMeHandler
 */
public class GetMeHandler extends Handler{

    static void handle(RequestResponsePair exchange, Database db) {
        try {
            String username = Authenticator.authenticate(exchange.getRequest());
            if(username == null) {
                MessageResponse response = new MessageResponse(false, HttpStatus.UNAUTHORIZED_401, "Authentication Failed");
                Handler.sendJson(response, exchange);
                return;
            }

            User me = db.getUser(username);

            if(me == null) {
                MessageResponse response = new MessageResponse(false, HttpStatus.NOT_FOUND_404, "User Not Found");
                Handler.sendJson(response, exchange);
                return;
            }

            me.setPassword(null);
            UserDataResponse response = new UserDataResponse(true, HttpStatus.OK_200, me);
            Handler.sendJson(response, exchange);
        }
        catch(SQLException e){
            e.printStackTrace();
            MessageResponse response = new MessageResponse(false, HttpStatus.INTERNAL_SERVER_ERROR_500, "Server Error");
            Handler.sendJson(response, exchange);
        }
    }
}
