package org.flashback.types;

public class MessageResponse extends ServerResponse{
    private String message;

    public MessageResponse() {}
    public MessageResponse(boolean ok, int statusCode, String message) {
        super(ok, statusCode);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
