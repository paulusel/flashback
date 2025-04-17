package org.flashback.server.handlers;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.server.RequestResponsePair;
import org.flashback.types.MessageResponse;

public class RootDispatcher implements Runnable {
    private static final HashMap<String, DispatchHandler> handlers = new HashMap<>();

    static {
        handlers.put("getMe", GetMeHandler::handle);
        handlers.put("uploadFile", FileUploadHandler::handle);
        handlers.put("auth", AuthHandler::handle);
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
                    MessageResponse response = new MessageResponse(false, HttpStatus.METHOD_NOT_ALLOWED_405, "Expected POST Request");
                    Handler.sendJson(response, exchange);
                    continue;
                }

                String path = exchange.getRequest().getRequestURI().substring(1);
                var handler = handlers.get(path);

                if(handler == null) {
                    MessageResponse response = new MessageResponse(false, HttpStatus.NOT_FOUND_404, "API Method Not Found");
                    Handler.sendJson(response, exchange);
                    continue;
                }

                handler.handle(exchange);
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FunctionalInterface
    private interface DispatchHandler {
        void handle(RequestResponsePair exchange);
    }
}
