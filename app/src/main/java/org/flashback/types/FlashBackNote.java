package org.flashback.types;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FlashBackNote {
    private Integer noteId;
    private String note;
    private Date modified;
    private Date created;
    private List<String> tags = new ArrayList<>();
    private List<NoteFile> files = new ArrayList<>();

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

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<NoteFile> getFiles() {
        return files;
    }

    public void setFiles(List<NoteFile> files) {
        this.files = files;
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

    public void addFile(NoteFile file) {
        this.files.add(file);
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }
}
