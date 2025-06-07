package org.flashback.types;

public class NoteFile {
    public enum Type { 
        PHOTO(1),
        VIDEO(2),
        AUDIO(3),
        DOCUMENT(4);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static Type typeOf(int value) {
            if(value == 1) return PHOTO;
            else if(value == 2) return VIDEO;
            else if(value == 3) return AUDIO;
            else return DOCUMENT;
        }
    }


    private String hash;
    private Type type;
    private String extension;
    private Long size;
    transient private String telegramFileId;

    public String getHash() {
        return this.hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
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
        return this.getHash() + "." + this.getExtension();
    }

    public Type getFileType() {
        return this.type;
    }

    public void setFileType(Type type) {
        this.type = type;
    }
}
