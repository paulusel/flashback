package org.flashback.types;

public class NoteParameter {

    private Integer noteid;
    private String value;

    public NoteParameter() {}

    public NoteParameter(Integer noteId, String value) {
        this.noteid = noteId;
        this.value = value;
    }

    public Integer getNoteId() {
        return this.noteid;
    }

    public void setNoteid(Integer noteId) {
        this.noteid = noteId;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
