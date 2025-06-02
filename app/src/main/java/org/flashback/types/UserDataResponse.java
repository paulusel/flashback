package org.flashback.types;

public class UserDataResponse extends ServerResponse{
    private FlashBackUser user;

    public UserDataResponse() {}
    public UserDataResponse(boolean ok, int statusCode, FlashBackUser user) {
        super(ok, statusCode);
        this.user = user;
    }

    public FlashBackUser getUser() {
        return user;
    }

    public void setUser(FlashBackUser user) {
        this.user = user;
    }
}
