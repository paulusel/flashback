package org.flashback.server.handlers;

import org.flashback.types.User;

import java.sql.SQLException;

import org.flashback.database.Database;
import org.flashback.server.RequestResponsePair;

import com.google.gson.Gson;

/**
 * GetMeHandler
 */
public class GetMeHandler extends Handler{

    static void handle(RequestResponsePair exchange) {
        try(Database db = Database.getDatabase()) {
            User me = db.getUser("mathos");
            if(me == null) {
                ErrorHandler.handle(404, "User Not Found", exchange);
            }
            else {
                String response = new Gson().toJson(me);
                exchange.getResponse().setHeader("Content-Type", "application/json");
                Handler.sendResponse(200, response, exchange);
            }
        }
        catch(SQLException e){
            e.printStackTrace();
            ErrorHandler.handle(500, "Internal Server Error", exchange);
        }
    }
}
