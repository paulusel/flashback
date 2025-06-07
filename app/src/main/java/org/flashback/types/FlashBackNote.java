package org.flashback.types;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FlashBackNote {
    private Integer noteId;
    private String note;
    private Date modified;
    private Date created;
    private final List<String> tags = new ArrayList<>();
    private final List<NoteFile> files = new ArrayList<>();

    public Integer getNoteId() {
        return noteId;
    }

    public void setNoteId(Integer itemid) {
        this.noteId = itemid;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<NoteFile> getFiles() {
        return files;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }


    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
