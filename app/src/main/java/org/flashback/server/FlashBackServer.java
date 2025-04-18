package org.flashback.server;

import org.flashback.server.handlers.RootDispatcher;
import org.flashback.types.RequestResponsePair;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;

public class FlashBackServer {
    private static FlashBackServer server;

    private final int port = 8080;
    private final int nThreads = 2;
    private final int queueSize = 100;

    private final BlockingQueue<RequestResponsePair> queue = new ArrayBlockingQueue<>(queueSize);
    private Server httpServer = new Server(port);
    private final ArrayList<Thread> threads = new ArrayList<>();

    public static FlashBackServer getServer() {
        if(server == null) {
            server = new FlashBackServer();
        }

        return server;
    }

    private FlashBackServer() {};

    public void startService() throws Exception{

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new FlashbackHttpServlet(queue), "/*");
        httpServer.setHandler(context);

        for(int i = 0; i < nThreads; ++i) {
            Thread consumer = new Thread(new RootDispatcher(queue));
            threads.add(consumer);
            consumer.start();
        }

        httpServer.start();
    }

    public void stopService() throws Exception{
        httpServer.stop();
    }
}
