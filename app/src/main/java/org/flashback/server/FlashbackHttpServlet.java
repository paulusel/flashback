package org.flashback.server;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.helpers.GenericHandler;
import org.flashback.types.MessageResponse;
import org.flashback.types.RequestResponsePair;

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
        // CORS Headers
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");

        if (req.getMethod().equalsIgnoreCase("OPTIONS")) {
            res.setHeader("Content-Type", "application/json");
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        var exchange = new RequestResponsePair(req, res);
        if(!queue.offer(exchange)) {
            GenericHandler.sendResponse(
                new MessageResponse(false, HttpStatus.SERVICE_UNAVAILABLE_503, "Server Busy"),
                exchange);
            return;
        }

        req.startAsync().setTimeout(30000); // 30s timeout
    }
}
