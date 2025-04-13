package org.flashback;

import org.flashback.server.FlashBackServer;

public class Main {
    public static void main(String[] args) {
        try {
            FlashBackServer.getServer().startService();
            //TODO: Start bot and start polling
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
