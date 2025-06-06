package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.GenericHandler;
import org.flashback.types.BotCodeResponse;
import org.flashback.types.RequestResponsePair;

public class GetBotCodeHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            Integer userId = Authenticator.authenticate(exchange.request);
            String otpToken = Authenticator.generateOtpToken(userId);
            BotCodeResponse response = new BotCodeResponse(true, HttpStatus.OK_200, otpToken);
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
