package org.flashback.server.handlers;

import java.util.concurrent.BlockingQueue;

import org.flashback.server.RequestResponsePair;

public class RootHandler extends Handler implements Runnable {
    private BlockingQueue<RequestResponsePair> queue;

    public RootHandler(BlockingQueue<RequestResponsePair> queue) {
        this.queue = queue;
    }

    @Override
    public void run(){
        while (!Thread.interrupted()) {
            try {
                handle(queue.take());
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handle(RequestResponsePair exchange) {

        try {

            if(!exchange.getRequest().getMethod().equals("POST")) {
                ErrorHandler.handle(400, "POST request is expected", exchange);
                return;
            }

            String path = exchange.getRequest().getRequestURI().substring(1);
            switch (path) {
                case "uploadFile":
                    FileUploadHandler.handle(exchange);
                    break;
                case "getMe":
                    GetMeHandler.handle(exchange);
                    break;
                default:
                    ErrorHandler.handle(400, "API method not recognized", exchange);
            }

        }
        finally {
            var async = exchange.getRequest().getAsyncContext();
            if (async != null) async.complete();
        }
    }
}
