package org.flashback.server.handlers;

import org.flashback.types.RequestResponsePair;
import org.eclipse.jetty.http.HttpStatus;
import org.flashback.auth.Authenticator;
import org.flashback.types.NoteResponse;
import org.flashback.types.FlashBackNote;
import org.flashback.database.Database;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.*;
import org.flashback.telegram.FlashBackBot;

public class AddNoteHandler {
    public static void handle(RequestResponsePair exchange) {
        try {
            Integer userId = Authenticator.authenticate(exchange.request);
            FlashBackNote note = null;
            try {
                note = NoteProcessor.extractNoteFromForm(exchange.request);
                Database.addNoteAndAssignId(userId, note);

                if(!note.getFiles().isEmpty()) {
                    NoteProcessor.postProcessFiles(note.getFiles());
                    Database.saveFiles(note.getFiles());
                    Database.addNoteFiles(note.getNoteId(), note.getFiles());
                }
            }
            catch(Exception e) {
                if(note != null && !note.getFiles().isEmpty()) {
                    NoteProcessor.cleanFiles(note.getFiles());
                }
                throw e;
            }

            FlashBackBot.trySendNoteToTelegram(userId, note);
            GenericHandler.sendResponse(new NoteResponse(true, HttpStatus.CREATED_201, note), exchange);
        }
        catch(FlashbackException e) {
            e.printStackTrace();
            GenericHandler.handleException(exchange, e);
        }
        catch(Exception e) {
            e.printStackTrace();
            GenericHandler.handleException(exchange, new FlashbackException());
        }
    }
}
