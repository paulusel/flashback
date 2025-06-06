package org.flashback.exceptions;

import org.eclipse.jetty.http.HttpStatus;

public class UserNotFoundException extends FlashbackException {
    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND_404, "user not found");
    }
}
