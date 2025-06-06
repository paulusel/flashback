package org.flashback.telegram;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.flashback.types.FlashBackNote;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.exceptions.UserNotFoundException;
import org.flashback.exceptions.VerificationException;
import org.flashback.helpers.Config;
import org.flashback.helpers.NoteProcessor;
import org.flashback.types.FlashBackUser;
import org.flashback.types.NoteFile;

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import org.apache.commons.io.FilenameUtils;

public class BotActionHandler implements LongPollingUpdateConsumer{
    private TelegramClient client;
    private String token;
    private HashMap<String, CommandHandler> commandHandlers = new HashMap<>();

    public BotActionHandler(TelegramClient client, String token) {
        this.client = client;
        this.token = token;
        commandHandlers.put("start", this::startCommandHandler);
        commandHandlers.put("search", this::searchCommandHandler);
        commandHandlers.put("register", this::registerCommandHandler);
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
    public FlashBackNote sendNote(FlashBackUser user, Integer noteId) throws TelegramApiException, FlashbackException {
        FlashBackNote note = Database.getNote(user.getUserId(), noteId);
        return sendNote(user, note);
    }

    public FlashBackNote sendNote(FlashBackUser user, FlashBackNote note) throws  TelegramApiException, FlashbackException {
        Long chatId = user.getTelegramUserId();
        if(chatId == null) throw new TelegramApiException();

        if(note.getFiles() != null) {
            List<NoteFileCategory> categories = NoteFileLabeler.categorize(user, note.getFiles());
            for(NoteFileCategory category: categories) {
                if(category.files.size() > 1) {
                    List<String> fileIds = sendMediaGroup(user, category.media);
                    for(int i = 0; i<fileIds.size(); ++i) {
                        category.files.get(i).setTelegramFileId(fileIds.get(i));
                    }
                }
                else {
                    String fileId;
                    switch (category.category) {
                        case NoteFileCategory.FileCategory.VISUAL:
                            fileId = sendVisualMedia(user, category.files.get(0));
                            break;
                        case NoteFileCategory.FileCategory.AUDIO:
                            fileId = sendAudioMedia(user, category.files.get(0));
                            break;
                        default:
                            fileId = sendDocumentMedia(user, category.files.get(0));
                            break;
                    }
                    category.files.get(0).setTelegramFileId(fileId);
                }
            }
        }

        sendNoteText(user.getTelegramChatId(), note);
        return note;
    }

    private String sendVisualMedia(FlashBackUser user, NoteFile file) throws TelegramApiException {
        Path userPath = Path.of(Config.getValue("uploads_dir")).resolve(String.valueOf(user.getUserId()));
        Path filePath = userPath.resolve(file.getFileId()).resolve(file.getFileName());

        InputFile inputFile = file.getTelegramFileId() == null
            ? new InputFile(new java.io.File(filePath.toString()), file.getFileName())
            : new InputFile(file.getTelegramFileId());

        if(file.getMimeType().startsWith("image/")) {
            SendPhoto sendPhoto = SendPhoto
                .builder()
                .chatId(user.getTelegramChatId())
                .photo(inputFile)
                .build();
            return client.execute(sendPhoto).getPhoto().getFirst().getFileId();
        }
        else {
            SendVideo sendVideo = SendVideo
                .builder()
                .chatId(user.getTelegramChatId())
                .video(inputFile)
                .build();
            return client.execute(sendVideo).getVideo().getFileId();
        }
    }

    private String sendAudioMedia(FlashBackUser user, NoteFile file) throws TelegramApiException {
        Path userPath = Path.of(Config.getValue("uploads_dir")).resolve(String.valueOf(user.getUserId()));
        Path filePath = userPath.resolve(file.getFileId()).resolve(file.getFileName());

        InputFile inputFile = file.getTelegramFileId() == null 
            ? new InputFile(new java.io.File(filePath.toString()), file.getFileName())
            : new InputFile(file.getTelegramFileId());

        SendAudio sendAudio = SendAudio
            .builder()
            .chatId(user.getTelegramChatId())
            .audio(inputFile)
            .build();
        return client.execute(sendAudio).getAudio().getFileId();
    }

    private String sendDocumentMedia(FlashBackUser user, NoteFile file) throws TelegramApiException {
        Path userPath = Path.of(Config.getValue("uploads_dir")).resolve(String.valueOf(user.getUserId()));
        Path filePath = userPath.resolve(file.getFileId()).resolve(file.getFileName());

        InputFile inputFile = file.getTelegramFileId() == null 
            ? new InputFile(new java.io.File(filePath.toString()), file.getFileName())
            : new InputFile(file.getTelegramFileId());

        SendDocument sendDocument = SendDocument
            .builder()
            .chatId(user.getTelegramChatId())
            .document(inputFile)
            .build();
        return client.execute(sendDocument).getDocument().getFileId();
    }

    private List<String> sendMediaGroup(FlashBackUser user, List<InputMedia> media) throws TelegramApiException {
        if(media == null) return null;
        var builder = SendMediaGroup
            .builder()
            .disableNotification(true)
            .chatId(user.getTelegramChatId());

        for(InputMedia medium: media) {
            builder.media(medium);
        }

        List<Message> messages = client.execute(builder.build());
        List<String> fileIds = new ArrayList<>();

        for(Message message : messages) {
            if(message.hasPhoto()) { fileIds.add(message.getPhoto().getFirst().getFileId()); }
            else if(message.hasVideo()) { fileIds.add(message.getVideo().getFileId()); }
            else if(message.hasAudio()) { fileIds.add(message.getAudio().getFileId()); }
            else if(message.hasDocument()) { fileIds.add(message.getDocument().getFileId()); }
        }

        return fileIds;
    }

    private void sendPlainMessage(Long chatId, String text) {
        try {
            SendMessage sendMsg = SendMessage
                .builder()
                .chatId(chatId)
                .disableNotification(true)
                .text(text)
                .build();
            client.execute(sendMsg);
        }
        catch(TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendProcessingStatus(Long chatId) throws TelegramApiException {
        SendChatAction sendChatAction = SendChatAction
            .builder()
            .chatId(chatId)
            .action("upload_document")
            .build();
        client.execute(sendChatAction);
    }

    private void sendNoteText(Long chatId, FlashBackNote note) throws TelegramApiException {
        String txt = note.getNote() == null ? "No note text" : note.getNote();
        if(!note.getTags().isEmpty()) {
            txt += "\n\nTags: " + String.join(", ", note.getTags());
        }

        InlineKeyboardButton removeButton = new InlineKeyboardButton("Remove");
        removeButton.setCallbackData(String.valueOf(note.getNoteId()));

        SendMessage sendMessage = SendMessage
            .builder()
            .disableNotification(true)
            .chatId(chatId)
            .replyMarkup(InlineKeyboardMarkup.builder().keyboardRow(new InlineKeyboardRow(removeButton)).build())
            .text(txt)
            .build();
        client.execute(sendMessage);
    }

    @Override
    public void consume(List<Update> updates) {
        HashMap<String, List<Message>> maps = new HashMap<>();
        for(Update update : updates) {
            if(update.hasMessage()) {
                Message message = update.getMessage();
                String groupId = message.getMediaGroupId();
                if(groupId != null) {
                    List<Message> group = maps.get(groupId);
                    if(group == null) {
                        group = new ArrayList<>();
                        maps.put(groupId, group);
                    }
                    group.add(message);
                }
                else { handleLoneMessage(message); }
            }
            else if(update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        }

        for(List<Message> group : maps.values()) {
            handleMediaGroup(group);
        }
    }

    private Integer getOriginalNoteId(Message message) {
        Message original = message.getReplyToMessage();
        if(original == null) return null;

        InlineKeyboardMarkup keyboard = original.getReplyMarkup();
        if(keyboard == null) return null;

        String data = keyboard.getKeyboard().get(0).get(0).getCallbackData();
        if(data == null) return null;

        return Integer.valueOf(data);
    }

    private void handleMediaGroup(List<Message> group) {
        Message samepleMessage = group.get(0);

        try {
            sendProcessingStatus(samepleMessage.getChatId());
            FlashBackUser user = Database.getUserByChatId(samepleMessage.getChatId());

            FlashBackNote note = new FlashBackNote();
            note.setNoteId(getOriginalNoteId(samepleMessage));

            String noteTxt = "";
            List<NoteFile> files = note.getFiles();

            for(Message message : group) {
                String caption = message.getCaption();
                if(caption != null && !caption.isEmpty()) {
                    noteTxt += "\n\n" + caption;
                }

                NoteFile file = getFileFromMessage(message);
                downloadFile(file);
                files.add(file);
            }

            note.setNote(noteTxt);

            try {
                NoteProcessor.postProcessFiles(user.getUserId(), note);
                Database.addOrUpdateNote(user.getUserId(), note);
            }
            catch(Exception e) {
                NoteProcessor.cleanFiles(user.getUserId(), note);
                throw e;
            }

            sendNote(user, note.getNoteId());
        }
        catch(UserNotFoundException e) {
            sendPlainMessage(samepleMessage.getChatId(), "You are not registered. Please start this bot with link");
        }
        catch(Exception e) {
            e.printStackTrace();
            sendPlainMessage(samepleMessage.getChatId(), "Unknown bot error. This error has been reported");
        }
    }

    private NoteFile getFileFromMessage(Message message) {
        NoteFile file = new NoteFile();
        if(message.hasPhoto()) {
            PhotoSize photo = message.getPhoto().getFirst();
            file.setTelegramFileId(photo.getFileId());
            file.setSize(photo.getFileSize().longValue());
        }
        else if(message.hasAudio()) {
            Audio audio = message.getAudio();
            file.setTelegramFileId(audio.getFileId());
            file.setExtension(FilenameUtils.getExtension(audio.getFileName()));
            file.setSize(audio.getFileSize());
        }
        else if(message.hasVideo()) {
            Video video = message.getVideo();
            file.setTelegramFileId(video.getFileId());
            file.setExtension(FilenameUtils.getExtension(video.getFileName()));
            file.setSize(video.getFileSize());
        }
        else if(message.hasDocument()) {
            Document doc = message.getDocument();
            file.setTelegramFileId(doc.getFileId());
            file.setExtension(FilenameUtils.getExtension(doc.getFileName()));
            file.setSize(doc.getFileSize());
        }
        return file;
    }

    /*
     * Parameter is modified internally.
     */
    private void downloadFile(NoteFile note) throws TelegramApiException {
        GetFile getFile = GetFile
            .builder()
            .fileId(note.getTelegramFileId())
            .build();

        File file = client.execute(getFile);
        String fileUrl = file.getFileUrl(this.token);
        String fileName = note.getTelegramFileId() + (note.getExtension() == null ? ".tmp" : note.getExtension());

        try(InputStream stream = new URI(fileUrl).toURL().openStream()) {
            NoteProcessor.processFile(note, stream, fileName);
        }
        catch(Exception e) {
            throw new TelegramApiException(e);
        }
    }

    private void handleLoneMessage(Message message) {
        try {
            if(message.hasText()) {
                // pure text messages
                String text = message.getText().trim();
                if(text.startsWith("/")) {
                    // commands
                    int pos = text.indexOf(' ');
                    String command = pos > 1 ? text.substring(1, pos) : text.substring(1);
                    String argument = pos > 1 ? text.substring(pos + 1) : "";
                    System.out.println("command: " + command + " argument: " + argument);
                    CommandHandler handler = commandHandlers.get(command);

                    if(handler != null) { handler.handle(argument, message); }
                    else { sendPlainMessage(message.getChatId(), "unrecognized command"); }
                }
                else { handleLoneTextMessage(message); }
            }
            else { handleLoneMediaMessage(message); }
        }
        catch(UserNotFoundException e) {
            sendPlainMessage(message.getChatId(), "You are not registered. Please start this bot with link");
        }
        catch(Exception e) {
            e.printStackTrace();
            sendPlainMessage(message.getChatId(), "Unknown bot error. This error has been reported");
        }
    }

    public void handleLoneTextMessage(Message message) throws TelegramApiException, FlashbackException {
        FlashBackUser user = Database.getUserByChatId(message.getChatId());

        FlashBackNote note = new FlashBackNote();
        note.setNoteId(getOriginalNoteId(message));

        String text = message.getText();
        if(text.startsWith("tag ") && note.getNoteId() != null) {
            // add tag to existing note
            String[] tagTokens = text.substring(4).split(" ");
            note.getTags().addAll(Arrays.asList(tagTokens));
        }
        else { note.setNote(text); }

        Database.addOrUpdateNote(user.getUserId(), note);
        sendNote(user, note.getNoteId());
    }

    public void handleLoneMediaMessage(Message message) throws TelegramApiException, FlashbackException {
        sendProcessingStatus(message.getChatId());
        FlashBackUser user = Database.getUserByChatId(message.getChatId());
        FlashBackNote note = new FlashBackNote();
        note.setNoteId(getOriginalNoteId(message));

        note.setNote(message.getCaption());
        NoteFile file = getFileFromMessage(message);
        downloadFile(file);
        note.getFiles().add(file);

        try {
            NoteProcessor.postProcessFiles(user.getUserId(), note);
            Database.addOrUpdateNote(user.getUserId(), note);
        }
        catch(Exception e) {
            NoteProcessor.cleanFiles(user.getUserId(), note);
            throw new TelegramApiException();
        }

        // Echo back the note
        sendNote(user, note.getNoteId());
    }

    public void handleCallbackQuery(CallbackQuery query) {
        try {
            AnswerCallbackQuery answerCallbackQuery = AnswerCallbackQuery
                .builder()
                .callbackQueryId(query.getId())
                .build();
            client.execute(answerCallbackQuery);

            Long chatId = query.getMessage().getChatId();
            FlashBackUser user = Database.getUserByChatId(chatId);

            DeleteMessage deleteMessage = DeleteMessage
                .builder()
                .chatId(chatId)
                .messageId(query.getMessage().getMessageId())
                .build();
            client.execute(deleteMessage);

            Integer noteId = Integer.valueOf(query.getData());
            Database.deleteNote(user.getUserId(), noteId);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    public void startCommandHandler(String token, Message message) {
        sendPlainMessage(message.getChatId(),
            "Welcome. You can now send notes. And also see notes sent from elsewhere here");
    }

    public void registerCommandHandler(String token, Message message) throws TelegramApiException {
        try {
            System.out.println("Token: " + token);
            Integer userId =  Authenticator.verifyOtpToken(token);
            FlashBackUser user = new FlashBackUser();
            user.setUserId(userId);
            user.setTelegramChatId(message.getChatId());
            user.setTelegramUserId(message.getFrom().getId());
            Database.updateUser(userId, user);
            sendPlainMessage(message.getChatId(),
                "Welcome. You can now send notes. And also see notes sent from elsewhere here");
        }
        catch(VerificationException e) {
            sendPlainMessage(message.getChatId(), "Invalid or expired otp token. Please regenerate link and try again");
        }
        catch(Exception e) {
            e.printStackTrace();
            sendPlainMessage(message.getChatId(), "Unknown bot error. Please try again later");
        }
    }

    public void searchCommandHandler(String keyword, Message message) {
        if(keyword == null || keyword.isEmpty()) {
            sendPlainMessage(message.getChatId(), "No search keyworkd provided");
            return;
        }

        try {
            FlashBackUser user = Database.getUserByChatId(message.getChatId());
            for(FlashBackNote note : Database.searchNotes(user.getUserId(), keyword)) {
                sendNote(user, note);
            }
        }
        catch(UserNotFoundException e) {
            sendPlainMessage(message.getChatId(), "You are not registered. Please start this bot with link");
        }
        catch(Exception e) {
            sendPlainMessage(message.getChatId(), "Unknown server error. Please try again later");
        }
    }

    @FunctionalInterface
    private interface CommandHandler {
        public void handle(String argument, Message message)  throws TelegramApiException;
    }
}
