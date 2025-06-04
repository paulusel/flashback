package org.flashback.types;

import java.util.List;

public class MultipleNoteResponse extends ServerResponse { 
    private List<FlashBackNote> notes;

    public MultipleNoteResponse() {}

    public MultipleNoteResponse(boolean ok, int statusCode, List<FlashBackNote> notes) {
        super(ok, statusCode);
        this.notes = notes;
    }

    public void setNotes(List<FlashBackNote> notes) {
        this.notes = notes;
    }

    public List<FlashBackNote> getList() {
        return this.notes;
    }
}
