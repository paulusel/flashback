package org.flashback.types;

public class AuthResponse extends UserDataResponse {
    private String token;

    public AuthResponse() {}
    public AuthResponse(boolean ok, int statusCode, String token, FlashBackUser user) {
        super(ok, statusCode, user);
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
