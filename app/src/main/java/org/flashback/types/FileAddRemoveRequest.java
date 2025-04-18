package org.flashback.types;

public class FileAddRemoveRequest {
    private String fileId;
    private Integer memoId;

    public FileAddRemoveRequest() {}

    public FileAddRemoveRequest(String fileId, Integer memoId) {
        this.fileId = fileId;
        this.memoId = memoId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getMemoId() {
        return memoId;
    }

    public void setMemoId(Integer memoId) {
        this.memoId = memoId;
    }
}
