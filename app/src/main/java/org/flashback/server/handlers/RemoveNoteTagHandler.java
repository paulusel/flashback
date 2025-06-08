package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.GenericHandler;
import org.flashback.helpers.Json;
import org.flashback.telegram.FlashBackBot;
import org.flashback.types.FlashBackNote;
import org.flashback.types.MessageResponse;
import org.flashback.types.NoteParameter;
import org.flashback.types.RequestResponsePair;

public class RemoveNoteTagHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            Integer userId = Authenticator.authenticate(exchange.request);
            String json = GenericHandler.getRequestBodyString(exchange.request);
            NoteParameter parameter = Json.deserialize(json, NoteParameter.class);

            if(parameter.getNoteId() == null || parameter.getValue() == null) {
                throw new FlashbackException(HttpStatus.BAD_REQUEST_400, "missing note_id or file_hash field");
            }

            Database.removeNoteTag(userId, parameter.getNoteId(), parameter.getValue());
            FlashBackNote note = Database.getNote(userId, parameter.getNoteId());
            FlashBackBot.trySendNoteToTelegram(userId, note);

            MessageResponse response = new MessageResponse(true, HttpStatus.OK_200, "succssfully removed");
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
