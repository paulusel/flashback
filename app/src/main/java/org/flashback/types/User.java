package org.flashback.types;

/**
 * User
 */
public class User {

    private String userName;
    private String password;
    transient private String telegramUserId;

    public User() {}

    public User(String userName) {
        this.userName = userName;
    }

    public User(String uName, String telegram_id){
        this.telegramUserId = telegram_id;
        this.userName = uName;
    }

    public String getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(String telegramUserId) {
        this.telegramUserId = telegramUserId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
