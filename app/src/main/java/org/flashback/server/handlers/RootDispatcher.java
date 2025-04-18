package org.flashback.server.handlers;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.types.RequestResponsePair;
import org.flashback.exceptions.FlashbackException;

public class RootDispatcher implements Runnable {
    private static final HashMap<String, DispatchHandler> handlers = new HashMap<>();

    static {
        handlers.put("signup", SignupHandler::handle);
        handlers.put("deleteme", DeleteAccountHandler::handle);
        handlers.put("login", LoginHandler::handle);
        handlers.put("getme", GetMeHandler::handle);
        handlers.put("addnotes", AddMemoHandler::handle);
        handlers.put("getnotes", GetMemoHandler::handle);
        handlers.put("rmnotes", DeleteMemoHandler::handle);
        handlers.put("upload", FileUploadHandler::handle);
        handlers.put("rmfiles", RemoveFilesFromMemoHandler::handle);
        handlers.put("addfiles", AddFilesToMemoHandler::handle);
    }

    private final BlockingQueue<RequestResponsePair> queue;

    public RootDispatcher(BlockingQueue<RequestResponsePair> queue) {
        this.queue = queue;
    }

    @Override
    public void run(){
        while (!Thread.interrupted()) {
            try( RequestResponsePair exchange = queue.take()) {

                if(!exchange.getRequest().getMethod().equals("POST")) {
                    Handler.handleException(exchange, new FlashbackException(HttpStatus.METHOD_NOT_ALLOWED_405, "expected POST request"));
                    continue;
                }

                String path = exchange.getRequest().getRequestURI().substring(1);
                var handler = handlers.get(path);

                if(handler == null) {
                    Handler.handleException(exchange, new FlashbackException(HttpStatus.NOT_FOUND_404, "api method not found"));
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
