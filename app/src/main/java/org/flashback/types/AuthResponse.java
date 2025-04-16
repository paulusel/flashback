package org.flashback.types;

public class AuthResponse extends ServerResponse{
    private String token;
    private User user;

    public AuthResponse() {}
    public AuthResponse(boolean ok, int statusCode, String token, User user) {
        super(ok, statusCode);
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
