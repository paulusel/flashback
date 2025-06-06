package org.flashback.telegram;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.flashback.helpers.Config;
import org.flashback.types.FlashBackUser;
import org.flashback.types.NoteFile;

import org.telegram.telegrambots.meta.api.objects.media.InputMediaAudio;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;

public class NoteFileLabeler {
    public static List<NoteFileCategory> categorize(FlashBackUser user, List<NoteFile> files) {
        List<NoteFileCategory> categories = new ArrayList<>();

        var audios = new NoteFileCategory(NoteFileCategory.FileCategory.AUDIO);
        var visuals = new NoteFileCategory(NoteFileCategory.FileCategory.VISUAL);
        var documents = new NoteFileCategory(NoteFileCategory.FileCategory.DOCUMENT);

        Path userPath = Path.of(Config.getValue("uploads_dir")).resolve(String.valueOf(user.getUserId()));

        for(NoteFile file : files) {
            String mime = file.getMimeType();

            if(mime.startsWith("image/")) { visuals.files.add(file); }
            else if(mime.startsWith("video/")) { visuals.files.add(file); }
            else if(mime.startsWith("audio/")) { audios.files.add(file); }
            else { documents.files.add(file); }
        }

        if(!visuals.files.isEmpty()) {
            categories.add(visuals);
            if(visuals.files.size() > 1) {
                for(var file : visuals.files) {
                    Path filePath = userPath.resolve(file.getFileId()).resolve(file.getFileName());
                    String telegramFileId = file.getTelegramFileId();
                    String mime = file.getMimeType();

                    if(mime.startsWith("image/")) {
                        var builder = InputMediaPhoto.builder();
                        builder = (telegramFileId == null) ? builder.media(new File(filePath.toString()), file.getFileName())
                            : builder.media(telegramFileId);
                        visuals.media.add(builder.build());
                    }
                    else {
                        var builder = InputMediaVideo.builder();
                        builder = (telegramFileId == null) ? builder.media(new File(filePath.toString()), file.getFileName())
                            : builder.media(telegramFileId);
                        visuals.media.add(builder.build());
                    }
                }
            }
        }

        if(!audios.files.isEmpty()) {
            categories.add(audios);
            if(audios.files.size() > 1) {
                for(var file : audios.files) {
                    Path filePath = userPath.resolve(file.getFileId()).resolve(file.getFileName());
                    String telegramFileId = file.getTelegramFileId();

                    var builder = InputMediaAudio.builder();
                    builder = (telegramFileId == null) ? builder.media(new File(filePath.toString()), file.getFileName())
                        : builder.media(telegramFileId);
                    audios.media.add(builder.build());
                }
            }
        }

        if(!documents.files.isEmpty()) {
            categories.add(documents);
            if(documents.files.size() > 1) {
                for(var file : documents.files) {
                    Path filePath = userPath.resolve(file.getFileId()).resolve(file.getFileName());
                    String telegramFileId = file.getTelegramFileId();

                    var builder = InputMediaDocument.builder();
                    builder = (telegramFileId == null) ? builder.media(new File(filePath.toString()), file.getFileName())
                        : builder.media(telegramFileId);
                    documents.media.add(builder.build());
                }
            }
        }

        return categories;
    }
}
