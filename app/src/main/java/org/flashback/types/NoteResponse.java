package org.flashback.types;

/**
 * NoteResponse
 */
public class NoteResponse extends ServerResponse{
    private FlashBackNote note;

    public NoteResponse() {}

    public NoteResponse(boolean ok, int statusCode, FlashBackNote note) {
        super(ok, statusCode);
        this.note = note;
    }

    public FlashBackNote getNote() {
        return this.note;
    }

    public void setNote(FlashBackNote note) {
        this.note = note;
    }
}
