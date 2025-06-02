package org.flashback.telegram;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.flashback.types.FlashBackNote;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.FlashBackUser;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
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

    /*
     *  For new note, files of the note are modified to have fileIds from telegram 
     */
    public FlashBackNote sendNote(FlashBackUser user, FlashBackNote note) throws TelegramApiException {
        Long chatId = user.getTelegramUserId();
        if(chatId == null) throw new TelegramApiException();

        if(note.getFiles() != null) {
            List<NoteFileCategory> categories = NoteFileLabeler.categorize(user, note.getFiles());
            for(NoteFileCategory category: categories) {
                List<String> fileIds = sendMediaGroup(user, category.media);
                for(int i = 0; i<fileIds.size(); ++i) {
                    category.files.get(i).setFileId(fileIds.get(i));
                }
            }
        }

        sendMessageWithInlineInput(user, note);
        return note;
    }

    private List<String> sendMediaGroup(FlashBackUser user, List<InputMedia> media) throws TelegramApiException {
        if(media == null) return null;
        var builder = SendMediaGroup
            .builder()
            .chatId(user.getTelegramChatId());

        for(InputMedia medium: media) {
            builder.media(medium);
        }

        List<Message> messages = client.execute(builder.build());
        List<String> fileIds = new ArrayList<>();

        for(Message message : messages) {
            if(message.hasPhoto()) {
                PhotoSize largestPhoto = message.getPhoto().stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);
                fileIds.add(largestPhoto.getFileId());
            }
            else if(message.hasVideo()) {
                fileIds.add(message.getVideo().getFileId());
            }
            else if(message.hasAudio()) {
                fileIds.add(message.getAudio().getFileId());
            }
            else if(message.hasDocument()) {
                fileIds.add(message.getDocument().getFileId());
            }
        }

        return fileIds;
    }

    private void sendPlainMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMsg = SendMessage
            .builder()
            .chatId(chatId)
            .text(text)
            .build();
        client.execute(sendMsg);
    }

    private void sendMessageWithInlineInput(FlashBackUser user, FlashBackNote note){

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
                    sendPlainMessage(message.getChatId(), "unreacognized command");
                }
            }
        }
    }

    public void handleCallbackQuery(CallbackQuery query) throws TelegramApiException {
        Long chatId = query.getMessage().getChatId();
        sendPlainMessage(chatId, "comming soon");
    }

    public void startCommandHandler(String token, Message message) throws TelegramApiException {
        if(token == null || token.isEmpty()) {
            sendPlainMessage(message.getChatId(), "please generate signin token before starting this bot");
        }
        else {
            try {
                BotUserRegistrationHandler.registerTelegramUser(message.getFrom().getId(), message.getChatId(), token);
                sendPlainMessage(message.getChatId(),
                    "Welcome. You can now send notes. And also see notes sent from elsewhere here");
            }
            catch(FlashbackException e) {
                sendPlainMessage(message.getChatId(), e.getMessage());
            }
            catch(Exception e) {
                sendPlainMessage(message.getChatId(), "unknown server error. please try again later");
            }
        }
    }

    @FunctionalInterface
    private interface CommandHandler {
        public void handle(String argument, Message message)  throws TelegramApiException;
    }
}
