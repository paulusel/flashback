package org.flashback.exceptions;

import org.eclipse.jetty.http.HttpStatus;

public class FlashbackException extends Exception {
    private final int statusCode;
    public FlashbackException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public FlashbackException(String message) {
        this(HttpStatus.BAD_REQUEST_400, message);
    }

    public FlashbackException() {
        this(HttpStatus.INTERNAL_SERVER_ERROR_500, "internal server error");
    }

    public int getStatusCode() {
        return statusCode;
    }
}
