package org.flashback.types;

public class BotLinkResponse extends ServerResponse {
    private String link;

    public BotLinkResponse(boolean ok, int statusCode, String link) {
        super(ok, statusCode);
        this.link = link;
    }

    public String getLink() {
        return this.link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
