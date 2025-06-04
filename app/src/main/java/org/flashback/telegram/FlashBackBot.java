package org.flashback.telegram;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.flashback.database.Database;
import org.flashback.helpers.Config;
import org.flashback.types.FlashBackNote;
import org.flashback.types.FlashBackUser;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;


public class FlashBackBot {

    private static TelegramClient client;
    private static TelegramBotsLongPollingApplication botApp;
    private static BotActionHandler botActionHandler;
    private static String token;

    public static void start() throws Exception {
        FlashBackBot.token = Config.getValue("bot_token");
        FlashBackBot.client = new OkHttpTelegramClient(token);
        FlashBackBot.botActionHandler = new BotActionHandler(client, token);

        try {
            botApp = new TelegramBotsLongPollingApplication();
            botApp.registerBot(token, botActionHandler);
        }
        catch(TelegramApiException e) {
            throw new Exception("failed to initialize bot");
        }
    }

    public static void stop() throws Exception {
        botApp.close();
    }

    public static FlashBackNote sendNote(FlashBackUser user, Integer noteId) throws Exception {
        FlashBackNote note = Database.getNote(user.getUserId(), noteId);
        return botActionHandler.sendNote(user, note);
    }

    public static String getBotUserName() throws Exception {
        return botActionHandler.getBotUserName();
    }
}
