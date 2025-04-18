package org.flashback;

import org.flashback.database.Database;
import org.flashback.helpers.Config;
import org.flashback.server.FlashBackServer;

public class Main {
    public static void main(String[] args) {
        try {
            Config.configure();
            Database.init();
            FlashBackServer.getServer().startService();
            //TODO: Start bot and start polling
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
