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
        handlers.put("addnote", AddNoteHandler::handle);
        handlers.put("getnote", GetNoteHandler::handle);
        handlers.put("rmnote", DeleteNoteHandler::handle);
        handlers.put("download", FileDownloadHandler::handle);
        handlers.put("addfile", AddFilesToNoteHandler::handle);
    }

    @Override
    public void run(){
        while (!Thread.interrupted()) {
            try( RequestResponsePair exchange = queue.take()) {

                if(!exchange.getRequest().getMethod().equals("POST")) {
                    GenericHandler.handleException(exchange, new FlashbackException(HttpStatus.METHOD_NOT_ALLOWED_405, "expected POST request"));
                    continue;
                }

                String path = exchange.getRequest().getRequestURI().substring(1);
                var handler = handlers.get(path);

                if(handler == null) {
                    GenericHandler.handleException(exchange, new FlashbackException(HttpStatus.NOT_FOUND_404, "api method not found"));
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
