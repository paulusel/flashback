package org.flashback.telegram;

import java.util.ArrayList;
import java.util.List;

import org.flashback.types.FlashBackUser;
import org.flashback.types.FlashBackFile;

public class NoteFileLabeler {
    public static List<NoteFileCategory> categorize(FlashBackUser user, List<FlashBackFile> files) {

        var audios = new NoteFileCategory(NoteFileCategory.Category.AUDIO);
        var visuals = new NoteFileCategory(NoteFileCategory.Category.VISUAL);
        var documents = new NoteFileCategory(NoteFileCategory.Category.DOCUMENT);


        for(FlashBackFile file : files) {

            switch (file.getFileType()) {
                case FlashBackFile.Type.PHOTO:
                case FlashBackFile.Type.VIDEO:
                    visuals.files.add(file);
                    break;
                case FlashBackFile.Type.AUDIO:
                    audios.files.add(file);
                    break;
                default:
                    documents.files.add(file);
                    break;
            }
        }

        List<NoteFileCategory> categories = new ArrayList<>();
        if(!visuals.files.isEmpty()) { categories.add(visuals); }
        if(!audios.files.isEmpty()) { categories.add(audios); }
        if(!documents.files.isEmpty()) { categories.add(documents); }

        return categories;
    }
}
