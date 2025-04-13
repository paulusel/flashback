package org.flashback.server;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.flashback.server.handlers.ErrorHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FlashbackHttpServlet extends HttpServlet {
    private final BlockingQueue<RequestResponsePair> queue;

    public FlashbackHttpServlet(BlockingQueue<RequestResponsePair> queue) {
        this.queue = queue;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        var exchange = new RequestResponsePair(req, res);
        if(!queue.offer(exchange)) {
            ErrorHandler.handle(500, "Server Busy", exchange);
            return;
        }

        req.startAsync().setTimeout(30000); // 30s timeout
    }
}
