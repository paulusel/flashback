package org.flashback.telegram;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.Config;
import org.flashback.types.FlashBackNote;
import org.flashback.types.FlashBackUser;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;


public class FlashBackBot {

    private static TelegramClient client;
    private static TelegramBotsLongPollingApplication botApp;
    private static BotActionHandler botActionHandler;
    private static String token;
    private static ExecutorService noteSender = Executors.newSingleThreadExecutor();

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

    public static void trySendNoteToTelegram(Integer userId, FlashBackNote note) {
        try{
            FlashBackUser user = Database.getUserByUserId(userId);
            if(user.getTelegramChatId() != null) {
                noteSender.execute(() -> {
                    try {
                        FlashBackNote not = botActionHandler.sendNote(user, note);
                        Database.saveTelegramFileIds(not.getFiles());
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        catch(FlashbackException e){ 
            e.printStackTrace();
            return;
        }
    }

    public static String getBotUserName() throws Exception {
        return botActionHandler.getBotUserName();
    }

}
