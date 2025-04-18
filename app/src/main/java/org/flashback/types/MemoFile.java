package org.flashback.types;

public class MemoFile {

    private String fileId;
    private String originalName;
    transient private String telegramFileId;

    public MemoFile() {};

    public MemoFile(String fileId, String telegramFileId, String originalName){
        this.fileId = fileId;
        this.telegramFileId = telegramFileId;
        this.originalName = originalName;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getTelegramFileId() {
        return telegramFileId;
    }

    public void setTelegramFileId(String telegramFileID) {
        this.telegramFileId = telegramFileID;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String name) {
        this.originalName = name;
    }
}
