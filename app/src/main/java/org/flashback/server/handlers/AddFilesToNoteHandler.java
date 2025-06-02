package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.FlashBackNote;
import org.flashback.types.NoteResponse;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.NoteProcessor;
import org.flashback.helpers.GenericHandler;

public class AddFilesToNoteHandler {

    public static void handle(RequestResponsePair exchange) {
        try {
            Integer userId = Authenticator.authenticate(exchange.getRequest());
            FlashBackNote note = null;
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
            GenericHandler.sendResponse(new NoteResponse(true, HttpStatus.CREATED_201, note), exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }
}
