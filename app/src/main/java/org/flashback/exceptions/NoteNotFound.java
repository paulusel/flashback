package org.flashback.exceptions;

import org.eclipse.jetty.http.HttpStatus;

public class NoteNotFound extends FlashbackException {
    public NoteNotFound() {
        super(HttpStatus.NOT_FOUND_404, "Note not found");
    }
}
