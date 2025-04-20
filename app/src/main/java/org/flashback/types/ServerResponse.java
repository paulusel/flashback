package org.flashback.types;

import org.eclipse.jetty.http.HttpStatus;

public class ServerResponse {

    private boolean ok;
    private String message;
    private transient int statusCode;

    public ServerResponse() {}

    public ServerResponse(boolean ok) {
        this(ok, ok ? HttpStatus.OK_200 : HttpStatus.BAD_REQUEST_400);
    }

    public ServerResponse(boolean ok, int statusCode) {
        this(ok, statusCode, null);
    }

    public ServerResponse(boolean ok, int statusCode, String message){
        this.ok = ok;
        this.statusCode = statusCode;
        this.message = message;
    }

    public boolean getOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
