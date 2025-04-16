package org.flashback.types;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Memo {

    private transient Integer id;
    private String memoId;
    private ArrayList<String> tags;
    private String note;
    private LocalDateTime modified;
    private ArrayList<MemoFile> files;

    public Memo() {}

    public Memo(Integer id, String memoId, ArrayList<String> tags, String note,
        LocalDateTime modified, ArrayList<MemoFile> files) {

        this.id = id;
        this.memoId = memoId;
        this.tags = tags;
        this.note = note;
        this.modified = modified;
        this.files = files;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMemoId() {
        return memoId;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public ArrayList<MemoFile> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<MemoFile> files) {
        this.files = files;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }
}
