package org.flashback.server.handlers;

import org.flashback.types.RequestResponsePair;
import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.types.NoteResponse;
import org.flashback.types.Note;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.*;

public class AddNoteHandler {
    public static void handle(RequestResponsePair exchange) {
        try {
            Long userId = Authenticator.authenticate(exchange.getRequest());
            Note note = null;
            try {
                note = NoteProcessor.extractNoteFromForm(exchange.getRequest());
                Database.addNote(userId, note);
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
