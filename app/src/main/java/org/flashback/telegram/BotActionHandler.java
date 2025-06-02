package org.flashback.telegram;

import java.util.HashMap;

import org.flashback.types.Note;
import org.flashback.types.FlashBackUser;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class BotActionHandler implements LongPollingSingleThreadUpdateConsumer {
    private TelegramClient client;
    private HashMap<String, CommandHandler> commandHandlers = new HashMap<>();

    public BotActionHandler(TelegramClient client) {
        this.client = client;
        commandHandlers.put("start", this::startCommandHandler);
    }


    public String getBotUserName() throws Exception {
        GetMe getMe = GetMe
            .builder()
            .build();

        User user = client.execute(getMe);
        return user.getUserName();
    }

    public void sendNote(FlashBackUser user, Note note) {
        Long chatId = user.getTelegramUserId();
        if(chatId == null) return;

        if(note.getFiles() != null) {
            //sendMessage(user, note);
        }
    }

    private void sendSimpleMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMsg = SendMessage
            .builder()
            .chatId(chatId)
            .text(text)
            .build();
        client.execute(sendMsg);
    }

    @Override
    public void consume(Update update) {
        try {
            if(update.hasMessage()) {
                handleMessage(update.getMessage());
            }
            else if(update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        }
        catch(TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void handleMessage(Message message) throws TelegramApiException {
        if(message.hasText()) {
            String text = message.getText().trim();
            if(text.startsWith("/")) {
                int pos = text.indexOf(' ');
                String command = pos > 1 ? text.substring(1, pos) : "";
                String argument = pos > 1 ? text.substring(pos + 1) : "";
                CommandHandler handler = commandHandlers.get(command);
                if(handler != null) {
                    handler.handle(argument, message);
                }
                else {
                    sendSimpleMessage(message.getChatId(), "unreacognized command");
                }
            }
        }
    }

    public void handleCallbackQuery(CallbackQuery query) throws TelegramApiException {
        Long chatId = query.getMessage().getChatId();
        sendSimpleMessage(chatId, "comming soon");
    }

    public void startCommandHandler(String token, Message message) throws TelegramApiException {
        if(token == null || token.isEmpty()) {
            sendSimpleMessage(message.getChatId(), "please generate signin token before starting this bot");
        }
        else {
            try {
                UserRegistration.registerTelegramUser(message.getFrom().getId(), message.getChatId(), token);
                sendSimpleMessage(message.getChatId(),
                    "Welcome. You can now send notes. And also see notes sent from elsewhere here");
            }
            catch(Exception e) {
                sendSimpleMessage(message.getChatId(), "token not recognized or expired");
            }
        }
    }

    @FunctionalInterface
    private interface CommandHandler {
        public void handle(String argument, Message message)  throws TelegramApiException;
    }
}
