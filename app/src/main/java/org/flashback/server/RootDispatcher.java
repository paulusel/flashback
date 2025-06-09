package org.flashback.server;

import org.flashback.server.handlers.*;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.types.RequestResponsePair;
import org.flashback.exceptions.FlashbackException;
import org.flashback.helpers.GenericHandler;

public class RootDispatcher implements Runnable {
    private HashMap<String, DispatchHandler> handlers = new HashMap<>();

    private final BlockingQueue<RequestResponsePair> queue;

    public RootDispatcher(BlockingQueue<RequestResponsePair> queue) {
        this.queue = queue;

        handlers.put("signup", SignupHandler::handle);
        handlers.put("deleteme", DeleteAccountHandler::handle);
        handlers.put("login", LoginHandler::handle);
        handlers.put("getme", GetMeHandler::handle);
        handlers.put("botcode", GetBotCodeHandler::handle);
        handlers.put("addnote", AddNoteHandler::handle);
        handlers.put("getnote", GetNoteHandler::handle);
        handlers.put("modnote", UpdateNoteHandler::handle);
        handlers.put("rmnote", DeleteNoteHandler::handle);
        handlers.put("file", FileDownloadHandler::handle);
        handlers.put("getfeed", GetFeedHandler::handle);
        handlers.put("search", SearchHandler::handle);
        handlers.put("rmfile", RemoveNoteFileHandler::handle);
        handlers.put("rmtag", RemoveNoteTagHandler::handle);
    }

    @Override
    public void run(){
        while (!Thread.interrupted()) {
            try(RequestResponsePair exchange = queue.take()) {
                String requestURI = exchange.request.getRequestURI().substring(1);
                int pos = requestURI.indexOf('/');
                String path = pos == -1 ? requestURI : requestURI.substring(0, pos);

                var handler = handlers.get(path);

                if(handler == null) {
                    GenericHandler.handleException(
                        exchange,
                        new FlashbackException(
                            HttpStatus.NOT_FOUND_404,
                            "api method not found"
                        )
                    );
                    continue;
                }

                handler.handle(exchange);
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @FunctionalInterface
    private interface DispatchHandler {
        void handle(RequestResponsePair exchange);
    }
}
