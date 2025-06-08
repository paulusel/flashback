package org.flashback.exceptions;

import org.eclipse.jetty.http.HttpStatus;

public class NoteFileNotFoundException extends FlashbackException {
    public NoteFileNotFoundException() {
        super(HttpStatus.NOT_FOUND_404, "file not found");
    }
}
