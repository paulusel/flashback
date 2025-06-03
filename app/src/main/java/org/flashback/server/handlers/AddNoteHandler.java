package org.flashback.server.handlers;

import org.flashback.types.RequestResponsePair;
import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.types.NoteResponse;
import org.flashback.types.FlashBackNote;
import org.flashback.types.FlashBackUser;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.*;
import org.flashback.telegram.FlashBackBot;

public class AddNoteHandler {
    public static void handle(RequestResponsePair exchange) {
        try {
            Integer userId = Authenticator.authenticate(exchange.getRequest());
            FlashBackNote note = null;
            try {
                note = NoteProcessor.extractNoteFromForm(exchange.getRequest());
                Database.addNoteAndAssignId(userId, note);
                NoteProcessor.postProcessFiles(userId, note);
            }
            catch(Exception e) {
                if(note != null) {
                    NoteProcessor.cleanFiles(userId, note);
                }
                throw e;
            }
            try{
                FlashBackUser user = Database.getUser(userId);
                if(user.getTelegramChatId() != null) {
                    note = FlashBackBot.getBot().sendNote(user, note.getNoteId());
                    Database.updateNote(userId, note);
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
            GenericHandler.sendResponse(new NoteResponse(true, HttpStatus.CREATED_201, note), exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
        catch(Exception e) {
            GenericHandler.handleException(exchange, new FlashbackException());
        }
    }
}
