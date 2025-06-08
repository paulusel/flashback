package org.flashback.telegram;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.flashback.types.FlashBackNote;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.exceptions.NoteNotFound;
import org.flashback.exceptions.UserNotFoundException;
import org.flashback.exceptions.VerificationException;
import org.flashback.helpers.Config;
import org.flashback.helpers.NoteProcessor;
import org.flashback.types.FlashBackUser;
import org.flashback.types.FlashBackFile;

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
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAudio;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
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
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

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
    public FlashBackNote sendNote(FlashBackUser user, FlashBackNote note) throws  TelegramApiException, FlashbackException {
        Long chatId = user.getTelegramUserId();
        if(chatId == null) throw new TelegramApiException();

        if(!note.getFiles().isEmpty()) {
            List<NoteFileCategory> categories = NoteFileLabeler.categorize(user, note.getFiles());
            for(NoteFileCategory category: categories) {
                if(category.files.size() > 1) {
                    List<String> fileIds = sendMediaGroup(user, category);
                    for(int i = 0; i<fileIds.size(); ++i) {
                        category.files.get(i).setTelegramFileId(fileIds.get(i));
                    }
                }
                else {
                    FlashBackFile file = category.files.get(0);
                    switch (category.category) {
                        case NoteFileCategory.Category.VISUAL:
                            file.setTelegramFileId(sendVisualMedia(user, file));
                            break;
                        case NoteFileCategory.Category.AUDIO:
                            file.setTelegramFileId(sendAudioMedia(user, file));
                            break;
                        default:
                            file.setTelegramFileId(sendDocumentMedia(user, file));
                            break;
                    }
                }
            }
        }

        sendNoteText(user.getTelegramChatId(), note);
        return note;
    }

    private InputFile getInputFile(FlashBackFile file) throws TelegramApiException {
        InputFile inputFile = null;
        if(file.getTelegramFileId() != null) {
            inputFile = new InputFile(file.getTelegramFileId());
        }
        else if(file.getHash() != null) {
            Path filePath = Path.of(Config.getValue("uploads_dir")).resolve(file.getHash()).resolve(file.getFileName());
            inputFile =  new InputFile(new java.io.File(filePath.toString()), file.getFileName());
        }
        else {
            throw new TelegramApiException("no file_id or name for media");
        }
        return inputFile;
    }

    private String sendVisualMedia(FlashBackUser user, FlashBackFile file) throws TelegramApiException {
        InputFile inputFile = getInputFile(file);
        if(file.getFileType() == FlashBackFile.Type.PHOTO) {
            SendPhoto sendPhoto = SendPhoto
                .builder()
                .chatId(user.getTelegramChatId())
                .photo(inputFile)
                .build();

            return client.execute(sendPhoto)
                .getPhoto()
                .stream()
                .max(Comparator.comparingInt(PhotoSize::getFileSize))
                .get()
                .getFileId();
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

    private String sendAudioMedia(FlashBackUser user, FlashBackFile file) throws TelegramApiException {
        InputFile inputFile = getInputFile(file);

        SendAudio sendAudio = SendAudio
            .builder()
            .chatId(user.getTelegramChatId())
            .audio(inputFile)
            .build();

        Message reply = client.execute(sendAudio);
        String fielId = null;

        if(reply.hasAudio()) {
            fielId = reply.getAudio().getFileId();
        }
        return fielId;
    }

    private String sendDocumentMedia(FlashBackUser user, FlashBackFile file) throws TelegramApiException {
        InputFile inputFile = getInputFile(file);

        SendDocument sendDocument = SendDocument
            .builder()
            .chatId(user.getTelegramChatId())
            .document(inputFile)
            .build();
        return client.execute(sendDocument).getDocument().getFileId();
    }

    private List<String> sendMediaGroup(FlashBackUser user, NoteFileCategory category) throws TelegramApiException {
        Path uploadDir = Path.of(Config.getValue("uploads_dir"));
        var builder = SendMediaGroup
            .builder()
            .disableNotification(true)
            .chatId(user.getTelegramChatId());

        if(category.category == NoteFileCategory.Category.VISUAL) {
            for(var file: category.files) {
                String telegramFileId = file.getTelegramFileId();
                Path filePath = file.getHash() != null 
                    ? uploadDir.resolve(file.getHash()).resolve(file.getFileName())
                    : null;

                if(file.getFileType() == FlashBackFile.Type.PHOTO) {
                    InputMediaPhoto media = telegramFileId == null
                        ? new InputMediaPhoto(new java.io.File(filePath.toString()), file.getFileName())
                        : new InputMediaPhoto(telegramFileId);
                    builder = builder.media(media);
                }
                else {
                    InputMediaVideo media = telegramFileId == null
                        ? new InputMediaVideo(new java.io.File(filePath.toString()), file.getFileName())
                        : new InputMediaVideo(telegramFileId);
                    builder = builder.media(media);
                }
            }
        }
        else if(category.category == NoteFileCategory.Category.AUDIO) {
            for(var file: category.files) {
                String telegramFileId = file.getTelegramFileId();
                Path filePath = file.getHash() != null 
                    ? uploadDir.resolve(file.getHash()).resolve(file.getFileName())
                    : null;

                InputMediaAudio media = telegramFileId == null
                    ? new InputMediaAudio(new java.io.File(filePath.toString()), file.getFileName())
                    : new InputMediaAudio(telegramFileId);
                builder = builder.media(media);
            }
        }
        else {
            for(var file: category.files) {
                String telegramFileId = file.getTelegramFileId();
                Path filePath = file.getHash() != null 
                    ? uploadDir.resolve(file.getHash()).resolve(file.getFileName())
                    : null;

                InputMediaDocument media = telegramFileId == null
                    ? new InputMediaDocument(new java.io.File(filePath.toString()), file.getFileName())
                    : new InputMediaDocument(telegramFileId);
                builder = builder.media(media);
            }
        }

        List<Message> messages = client.execute(builder.build());
        List<String> fileIds = new ArrayList<>();

        for(Message message : messages) {
            if(message.hasPhoto()) {
                fileIds.add(message.getPhoto()
                        .stream()
                        .max(Comparator.comparingInt(PhotoSize::getFileSize))
                        .get()
                        .getFileId()
                );
            }
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

            String noteTxt = "";

            for(Message message : group) {
                String caption = message.getCaption();
                if(caption != null && !caption.isEmpty()) {
                    noteTxt += "\n\n" + caption;
                }
                note.getFiles().add(getFileFromMessage(message));
            }

            note.setNote(noteTxt);
            note.setNoteId(getOriginalNoteId(samepleMessage));

            if(note.getNoteId() != null) {
                // existing note
                Database.updateNote(user.getUserId(), note);
                FlashBackNote existing = Database.getNote(user.getUserId(), note.getNoteId());
                existing.getFiles().addAll(note.getFiles());
                sendNote(user, existing);
            }
            else {
                // new note
                Database.addNoteAndAssignId(user.getUserId(), note);
                sendNote(user, note);
            }

            worker.execute(this.new DownloadTask(note.getNoteId(), note.getFiles()));
        }
        catch(UserNotFoundException e) {
            sendPlainMessage(samepleMessage.getChatId(), "You are not registered. Please start this bot with link");
        }
        catch(Exception e) {
            e.printStackTrace();
            sendPlainMessage(samepleMessage.getChatId(), "Unknown bot error. This error has been reported");
        }
    }

    private FlashBackFile getFileFromMessage(Message message) {
        FlashBackFile file = new FlashBackFile();
        if(message.hasPhoto()) {
            PhotoSize photo = message.getPhoto()
                .stream()
                .max(Comparator.comparingInt(PhotoSize::getFileSize))
                .get();

            file.setTelegramFileId(photo.getFileId());
            file.setSize(photo.getFileSize().longValue());
            file.setFileType(FlashBackFile.Type.PHOTO);
        }
        else if(message.hasAudio()) {
            Audio audio = message.getAudio();
            file.setTelegramFileId(audio.getFileId());
            file.setExtension(FilenameUtils.getExtension(audio.getFileName()));
            file.setSize(audio.getFileSize());
            file.setFileType(FlashBackFile.Type.AUDIO);
        }
        else if(message.hasVideo()) {
            Video video = message.getVideo();
            file.setTelegramFileId(video.getFileId());
            file.setExtension(FilenameUtils.getExtension(video.getFileName()));
            file.setSize(video.getFileSize());
            file.setFileType(FlashBackFile.Type.VIDEO);
        }
        else if(message.hasDocument()) {
            Document doc = message.getDocument();
            file.setTelegramFileId(doc.getFileId());
            file.setExtension(FilenameUtils.getExtension(doc.getFileName()));
            file.setSize(doc.getFileSize());
            file.setFileType(FlashBackFile.Type.DOCUMENT);
        }
        return file;
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
            Database.updateNote(user.getUserId(), note);
        }
        else if(text.startsWith("untag ") && note.getNoteId() != null) {
            try {
                Database.removeNoteTag(user.getUserId(), note.getNoteId(), text.substring(6));
            }
            catch(NoteNotFound e) {
                sendPlainMessage(user.getTelegramChatId(), "no such tag exists for the note");
            }
        }
        else {
            note.setNote(text);
            Database.addOrUpdateNote(user.getUserId(), note);
        }

        note = Database.getNote(user.getUserId(), note.getNoteId());
        sendNote(user, note);
    }

    public void handleLoneMediaMessage(Message message) throws TelegramApiException, FlashbackException {
        sendProcessingStatus(message.getChatId());
        FlashBackUser user = Database.getUserByChatId(message.getChatId());
        FlashBackNote note = new FlashBackNote();

        FlashBackFile file = getFileFromMessage(message);
        note.getFiles().add(file);

        note.setNote(message.getCaption());
        note.setNoteId(getOriginalNoteId(message));

        if(note.getNoteId() == null) {
            // new
            Database.addNoteAndAssignId(user.getUserId(), note);
            sendNote(user, note);
        }
        else {
            // existing
            Database.updateNote(user.getUserId(), note);
            FlashBackNote existing = Database.getNote(user.getUserId(), note.getNoteId());
            existing.getFiles().addAll(note.getFiles());
            sendNote(user, existing);
        }

        worker.execute(this.new DownloadTask(note.getNoteId(), Arrays.asList(file)));
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

            try { Database.deleteNote(user.getUserId(), noteId); }
            catch(NoteNotFound e) {}
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
            List<FlashBackNote> notes = Database.searchNotes(user.getUserId(), keyword);
            worker.execute(() -> {
                try {
                    for(FlashBackNote note : notes) {
                        sendNote(user, note);
                    }
                }
                catch(Exception e) {
                    System.err.println("Error sending search results");
                    e.printStackTrace();
                }
            });
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

    private class DownloadTask implements Runnable {
        private final Integer noteId;
        private final List<FlashBackFile> files;

        public DownloadTask(Integer noteId, List<FlashBackFile> files) {
            this.noteId = noteId;
            this.files = files;
        }

        @Override
        public void run() {
            try {
                for(var file : files) {
                    GetFile getFile = GetFile
                        .builder()
                        .fileId(file.getTelegramFileId())
                        .build();

                    File telegramFile = BotActionHandler.this.client.execute(getFile);
                    String fileUrl = telegramFile.getFileUrl(BotActionHandler.this.token);
                    String fileName = file.getTelegramFileId() + (file.getExtension() == null ? ".tmp" : file.getExtension());

                    try(InputStream stream = new URI(fileUrl).toURL().openStream()) {
                        NoteProcessor.processFile(file, stream, fileName);
                    }
                }

                NoteProcessor.postProcessFiles(files);
                Database.saveFiles(files);
                Database.addNoteFiles(noteId, files);
            }
            catch(Exception e) {
                e.printStackTrace();
                NoteProcessor.cleanFiles(files);
            }
        }

    }
}
