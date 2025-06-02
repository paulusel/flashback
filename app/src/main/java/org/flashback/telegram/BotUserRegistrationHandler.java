package org.flashback.telegram;

import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.FlashBackUser;

public class BotUserRegistrationHandler {
    public static void registerTelegramUser(Long telegramUserId, Long chatId, String token) throws FlashbackException {
        Integer userId =  Authenticator.verifyOtpToken(token);
        FlashBackUser user = new FlashBackUser();
        user.setUserId(userId);
        user.setTelegramChatId(chatId);
        user.setTelegramUserId(telegramUserId);
        Database.updateUser(userId, user);
    }
}
