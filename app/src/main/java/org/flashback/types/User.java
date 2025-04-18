package org.flashback.types;

/**
 * User
 */
public class User {

    private String username;
    private String password;
    transient private String telegramUserId;

    public User() {}

    public User(String userName) {
        this.username = userName;
    }

    public User(String uName, String telegram_id){
        this.telegramUserId = telegram_id;
        this.username = uName;
    }

    public String getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(String telegramUserId) {
        this.telegramUserId = telegramUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        this.username = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
