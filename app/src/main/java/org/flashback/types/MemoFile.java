package org.flashback.types;

public class MemoFile {

    private String fileId;
    transient private String telegramFileId;
    transient private String fileName;
    private String originalName;

    public MemoFile() {};

    public MemoFile(String fileId, String telegramFileId, String fileName, String originalName){
        this.fileId = fileId;
        this.telegramFileId = telegramFileId;
        this.fileName = fileName;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String name) {
        this.originalName = name;
    }
}
