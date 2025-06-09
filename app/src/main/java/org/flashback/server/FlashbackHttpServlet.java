package org.flashback.server;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.http.HttpStatus;
import org.flashback.exceptions.FlashbackException;
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
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");

        if (req.getMethod().equalsIgnoreCase("OPTIONS")) {
            res.setHeader("Content-Type", "application/json");
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }



        var exchange = new RequestResponsePair(req, res);
        if((!req.getMethod().equalsIgnoreCase("POST") && !req.getMethod().equalsIgnoreCase("GET")) ||
                (req.getMethod().equalsIgnoreCase("POST") && req.getRequestURI().startsWith("/file")) ||
                (req.getMethod().equalsIgnoreCase("GET") && !req.getRequestURI().startsWith("/file")))
        {
            GenericHandler.handleException(
                exchange,
                new FlashbackException(
                    HttpStatus.METHOD_NOT_ALLOWED_405,
                    "method not allowed: expect GET for 'file' and POST for everything else"
                )
            );
            return;
        }

        req.startAsync().setTimeout(30000); // 30s timeout
        if(!queue.offer(exchange)) {
            GenericHandler.sendResponse(
                new MessageResponse(false, HttpStatus.SERVICE_UNAVAILABLE_503, "Server Busy"),
                exchange);
            req.getAsyncContext().complete();
        }
    }
}
