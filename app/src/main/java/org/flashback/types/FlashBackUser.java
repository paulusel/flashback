package org.flashback.types;

public class FlashBackUser {

    private Integer userId;
    private String username;
    private String password;
    transient private Long telegramChatId;
    transient private Long telegramUserId;

    public FlashBackUser() {}

    public FlashBackUser(Integer userId) {
        this.userId = userId;
    }

    public FlashBackUser(Integer userId, Long telegramUserId, Long telegramChatId){
        this.telegramUserId = telegramUserId;
        this.telegramChatId = telegramChatId;
        this.userId = userId;
    }

    public String getUserName() {
        return username;
    }


    public void setUserName(String username) {
        this.username = username;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(Long telegramUserId) {
        this.telegramChatId = telegramUserId;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(Long telegramUserId) {
        this.telegramUserId = telegramUserId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
