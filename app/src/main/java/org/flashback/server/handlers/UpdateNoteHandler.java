package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MessageResponse;
import org.flashback.types.FlashBackNote;
import org.flashback.types.FlashBackUser;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.GenericHandler;
import org.flashback.helpers.NoteProcessor;
import org.flashback.telegram.FlashBackBot;

public class UpdateNoteHandler {

    public static void handle(RequestResponsePair exchange) {
        FlashBackNote note = null;
        try {
            Integer userId = Authenticator.authenticate(exchange.getRequest());
            try {
                note = NoteProcessor.extractNoteFromForm(exchange.getRequest());
                Database.addOrUpdateNote(userId, note);
                NoteProcessor.postProcessFiles(userId, note);
            }
            catch(Exception e) {
                if(note != null) {
                    NoteProcessor.cleanFiles(userId, note);
                }
            }

            try{
                FlashBackUser user = Database.getUser(userId);
                if(user.getTelegramChatId() != null) {
                    note = FlashBackBot.getBot().sendNote(user, note.getNoteId());
                    Database.updateNote(userId, note);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            GenericHandler.sendResponse(new MessageResponse(true, HttpStatus.OK_200, "updated"), exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }
}
