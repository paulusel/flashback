package org.flashback.types;

public class ServerResponse {

    private boolean ok;
    private transient int statusCode;

    public ServerResponse() {}
    public ServerResponse(boolean ok, int statusCode) {
        this.ok = ok;
        this.statusCode = statusCode;
    }

    public boolean getOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
