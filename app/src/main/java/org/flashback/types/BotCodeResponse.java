package org.flashback.types;

public class BotCodeResponse extends ServerResponse {
    private String code;

    public BotCodeResponse(boolean ok, int statusCode, String link) {
        super(ok, statusCode);
        this.code = link;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String link) {
        this.code = link;
    }
}
