package org.flashback.types;

public class MemoFile {

    private String fileId;
    private Integer tempId;
    private String originalName;
    private String mime_type;
    private Integer size;
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

    public Integer getTempId() {
        return tempId;
    }

    public void setTempId(Integer tempId) {
        this.tempId = tempId;
    }

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
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
