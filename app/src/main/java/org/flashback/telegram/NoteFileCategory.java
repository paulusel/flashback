package org.flashback.telegram;

import java.util.List;
import java.util.ArrayList;

import org.flashback.types.NoteFile;

public class NoteFileCategory {
    public enum Category {
        VISUAL,
        AUDIO,
        DOCUMENT
    }

    public final List<NoteFile> files = new ArrayList<>();
    public final Category category;

    public NoteFileCategory(Category category) {
        this.category = category;
    }
}
