package org.flashback.types;

/**
 * NoteResponse
 */
public class NoteResponse extends ServerResponse{
    private Note note;

    public NoteResponse() {}

    public NoteResponse(boolean ok, int statusCode, Note note) {
        super(ok, statusCode);
        this.note = note;
    }

    public Note getNote() {
        return this.note;
    }

    public void setNote(Note note) {
        this.note = note;
    }
}
