package org.flashback.types;

public class UserDataResponse extends ServerResponse{
    private User user;

    public UserDataResponse() {}
    public UserDataResponse(boolean ok, int statusCode, User user) {
        super(ok, statusCode);
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
