package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MessageResponse;
import org.flashback.types.Note;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.GenericHandler;
import org.flashback.helpers.NoteProcessor;

public class UpdateNoteHandler {

    public static void handle(RequestResponsePair exchange) {
        Note note = null;
        try {
            Long userId = Authenticator.authenticate(exchange.getRequest());
            try {
                note = NoteProcessor.extractNoteFromForm(exchange.getRequest());
                Database.updateNote(userId, note);
                NoteProcessor.postProcessFiles(userId, note);
            }
            catch(Exception e) {
                if(note != null) {
                    NoteProcessor.cleanFiles(userId, note);
                }
            }
            GenericHandler.sendResponse(new MessageResponse(true, HttpStatus.OK_200, "updated"), exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }
}
