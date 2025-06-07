package org.flashback.server.handlers;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.types.MessageResponse;
import org.flashback.types.FlashBackNote;
import org.flashback.types.RequestResponsePair;
import org.flashback.helpers.GenericHandler;
import org.flashback.helpers.NoteProcessor;
import org.flashback.telegram.FlashBackBot;

public class UpdateNoteHandler {

    public static void handle(RequestResponsePair exchange) {
        FlashBackNote note = null;
        try {
            Integer userId = Authenticator.authenticate(exchange.request);
            try {
                note = NoteProcessor.extractNoteFromForm(exchange.request);
                if(note.getNoteId() == null) {
                    throw new FlashbackException("noteId is note specfied in the form");
                }
                Database.updateNote(userId, note);

                if(!note.getFiles().isEmpty()) {
                    NoteProcessor.postProcessFiles(note.getFiles()); // async
                    Database.saveFiles(note.getFiles());
                    Database.addNoteFiles(note.getNoteId(), note.getFiles());
                }
            }
            catch(FlashbackException e) {
                if(note != null) {
                    NoteProcessor.cleanFiles(note.getFiles());
                }
                throw e;
            }

            note = Database.getNote(userId, note.getNoteId());
            FlashBackBot.trySendNoteToTelegram(userId, note);
            GenericHandler.sendResponse(new MessageResponse(true, HttpStatus.OK_200, "updated"), exchange);
        }
        catch(FlashbackException e) {
            GenericHandler.handleException(exchange, e);
        }
    }
}
