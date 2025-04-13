package org.flashback.server;

import org.flashback.server.handlers.RootHandler;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;

public class FlashBackServer {
    private static FlashBackServer server;

    private final int port = 8080;
    private final int nThreads = 2;
    private Server httpServer;
    private BlockingQueue<RequestResponsePair> queue;
    private ArrayList<Thread> threads;

    public static FlashBackServer getServer() {
        if(server == null) {
            server = new FlashBackServer();
        }

        return server;
    }

    private FlashBackServer() {
        queue = new ArrayBlockingQueue<>(100);
        httpServer = new Server(port);
    }

    public void startService() throws Exception{

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new FlashbackHttpServlet(queue), "/*");
        httpServer.setHandler(context);

        for(int i = 0; i < nThreads; ++i) {
            Thread consumer = new Thread(new RootHandler(queue));
            threads = new ArrayList<>();
            threads.add(consumer);
            consumer.start();
        }

        httpServer.start();
    }


    public void stopService() throws Exception{
        httpServer.stop();
    }
}
