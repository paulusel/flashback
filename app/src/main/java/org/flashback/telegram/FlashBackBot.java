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

    private static FlashBackBot bot;

    private TelegramClient client;
    private TelegramBotsLongPollingApplication botApp;
    private BotActionHandler botActionHandler;
    private String token;

    private FlashBackBot(String token) {
        this.token = token;
        this.client = new OkHttpTelegramClient(token);
        this.botActionHandler = new BotActionHandler(client, token);
    }

    public static void init() throws Exception {
        if(bot != null) return;
        FlashBackBot bt = new FlashBackBot(Config.getValue("bot_token"));
        bot = bt;
    }

    public static FlashBackBot getBot() throws Exception {
        if(bot == null) {
            throw new Exception("bot is null");
        }
        return bot;
    }

    public void start() throws Exception {
        this.botApp = new TelegramBotsLongPollingApplication();
        try {
            botApp.registerBot(token, botActionHandler);
        }
        catch(TelegramApiException e) {
            throw new Exception("failed to initialize bot");
        }
    }

    public void stop() throws Exception {
        this.botApp.close();
    }

    public TelegramClient getClient() {
        return client;
    }

    public FlashBackNote sendNote(FlashBackUser user, Integer noteId) throws Exception {
        FlashBackNote note = Database.getNote(user.getUserId(), noteId);
        return botActionHandler.sendNote(user, note);
    }

    public String getBotUserName() throws Exception {
        return botActionHandler.getBotUserName();
    }
}
