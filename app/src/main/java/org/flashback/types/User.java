package org.flashback.types;

/**
 * User
 */
public class User {

    transient private Integer userId;
    transient private String telegramUserId;
    private String userName;
    private String password;

    public User() {}

    public User(Integer id, String tgId, String uName, String pass){
        this.userId = id;
        this.telegramUserId = tgId;
        this.userName = uName;
        this.password = pass;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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
