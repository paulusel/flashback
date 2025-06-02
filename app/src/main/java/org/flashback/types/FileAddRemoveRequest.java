package org.flashback.types;

public class FileAddRemoveRequest {
    private String fileId;
    private Integer noteId;

    public FileAddRemoveRequest() {}

    public FileAddRemoveRequest(String fileId, Integer memoId) {
        this.fileId = fileId;
        this.noteId = memoId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public void setNoteId(Integer memoId) {
        this.noteId = memoId;
    }
}
