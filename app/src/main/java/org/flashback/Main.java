package org.flashback;

import org.flashback.database.Database;
import org.flashback.helpers.Config;
import org.flashback.server.FlashBackServer;
import org.flashback.telegram.Bot;

public class Main {
    public static void main(String[] args) {
        try {
            Config.init("config.ini");

            Database.init();
            FlashBackServer.init();
            Bot.init();

            FlashBackServer.getServer().startService();
            Bot.getBot().start();
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
