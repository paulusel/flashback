package org.flashback.exceptions;

import org.eclipse.jetty.http.HttpStatus;

public class VerificationException extends FlashbackException {
    public VerificationException() {
        super(HttpStatus.UNAUTHORIZED_401, "invalid or expired token");
    }
}
