package org.flashback.types;

public class NoteFile {

    private String fileId;
    private String extension;
    private String mime_type;
    private Long size;
    transient private String telegramFileId;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getMimeType() {
        return mime_type;
    }

    public void setMimeType(String mime_type) {
        this.mime_type = mime_type;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getTelegramFileId() {
        return telegramFileId;
    }

    public void setTelegramFileId(String telegramFileID) {
        this.telegramFileId = telegramFileID;
    }

    public String getExtension() {
        return this.extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getFileName() {
        return this.getFileId() + "." + this.getExtension();
    }
}
