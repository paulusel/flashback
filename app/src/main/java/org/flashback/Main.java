package org.flashback;

import java.nio.file.Path;

import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.helpers.Config;
import org.flashback.helpers.NoteProcessor;
import org.flashback.server.FlashBackServer;
import org.flashback.telegram.FlashBackBot;

public class Main {
    public static void main(String[] args) {
        try {
            Config.init(Path.of("config.ini"));

            Database.init();
            Authenticator.init();
            NoteProcessor.init();

            FlashBackServer.start();
            FlashBackBot.start();
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
