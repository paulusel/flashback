package org.flashback.types;

/**
 * User
 */
public class FlashBackUser {

    private String username;
    private Long userId;
    private String password;
    transient private Long telegramChatId;
    transient private Long telegramUserId;

    public FlashBackUser() {}

    public FlashBackUser(Long userId) {
        this.userId = userId;
    }

    public FlashBackUser(Long userId, Long telegramUserId, Long telegramChatId){
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
