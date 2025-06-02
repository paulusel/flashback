package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.GenericHandler;
import org.flashback.telegram.Bot;
import org.flashback.types.BotLinkResponse;
import org.flashback.types.RequestResponsePair;

public class GenerateBotLinkHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            Long userId = Authenticator.authenticate(exchange.getRequest());
            String otpToken = Authenticator.generateOtpToken(userId);
            String botAddress = Bot.getBot().getBotUserName();
            String link = "https://t.me/" + botAddress + "?start=" + otpToken;
            BotLinkResponse response = new BotLinkResponse(true, HttpStatus.OK_200, link);
            GenericHandler.sendResponse(response, exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
        catch(Exception e) {
            e.printStackTrace();
            GenericHandler.handleException(exchange, new FlashbackException());
        }
    }
}
