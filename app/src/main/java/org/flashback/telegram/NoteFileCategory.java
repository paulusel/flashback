package org.flashback.telegram;

import java.util.List;
import java.util.ArrayList;

import org.flashback.types.NoteFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;

public class NoteFileCategory {
    public enum FileCategory {
        VISUAL,
        AUDIO,
        DOCUMENT
    }

    public final List<InputMedia> media = new ArrayList<>();
    public final List<NoteFile> files = new ArrayList<>();

    public final FileCategory category;

    public NoteFileCategory(FileCategory category) {
        this.category = category;
    }
}
