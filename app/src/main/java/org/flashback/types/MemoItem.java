package org.flashback.types;

import java.util.Date;
import java.util.List;

public class MemoItem {

    public static enum ItemType {FOLDER, NOTE};

    private Integer itemId;
    private transient Integer tempId;
    private String name;
    private ItemType type;
    private Integer parent;
    private String note;
    private Date modified;
    private List<String> tags;
    private List<MemoFile> files;

    public MemoItem() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public Integer getParent() {
        return parent;
    }

    public void setParent(Integer parent) {
        this.parent = parent;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemid(Integer itemid) {
        this.itemId = itemid;
    }

    public Integer getTempId() {
        return tempId;
    }

    public void setTempId(Integer tempId) {
        this.tempId = tempId;
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

    public List<MemoFile> getFiles() {
        return files;
    }

    public void setFiles(List<MemoFile> files) {
        this.files = files;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }
}
