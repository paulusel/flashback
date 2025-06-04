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
            Path filePath = userPath.resolve(file.getFileId()).resolve(file.getFileName());
            String telegramFileId = file.getTelegramFileId();

            if(mime.startsWith("image/")) {
                var builder = InputMediaPhoto.builder();
                builder = (telegramFileId == null) ? builder.media(new File(filePath.toString()), file.getFileName())
                    : builder.media(telegramFileId);
                visuals.media.add(builder.build());
                visuals.files.add(file);
            }
            if(mime.startsWith("video/")) {
                var builder = InputMediaVideo.builder();
                builder = (telegramFileId == null) ? builder.media(new File(filePath.toString()), file.getFileName())
                    : builder.media(telegramFileId);
                visuals.media.add(builder.build());
                visuals.files.add(file);
            }
            else if(mime.startsWith("audio/")) {
                var builder = InputMediaAudio.builder();
                builder = (telegramFileId == null) ? builder.media(new File(filePath.toString()), file.getFileName())
                    : builder.media(telegramFileId);
                audios.media.add(builder.build());
                audios.files.add(file);
            }
            else {
                var builder = InputMediaDocument.builder();
                builder = (telegramFileId == null) ? builder.media(new File(filePath.toString()), file.getFileName())
                    : builder.media(telegramFileId);
                documents.media.add(builder.build());
                documents.files.add(file);

            }
        }

        if(!documents.media.isEmpty()) categories.add(documents);
        if(!audios.media.isEmpty()) categories.add(audios);
        if(!visuals.media.isEmpty()) categories.add(visuals);

        return categories;
    }
}
