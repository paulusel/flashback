package org.flashback.server;

import org.flashback.helpers.Config;
import org.flashback.types.RequestResponsePair;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;

public class FlashBackServer {
    private static int port;
    private static int nThreads;
    private static int queueSize;

    private static BlockingQueue<RequestResponsePair> queue;
    private static Server httpServer;
    private static final ArrayList<Thread> threads = new ArrayList<>();

    public static void start() throws Exception{

        String queueSize = Config.getValue("server_queue_size");
        String nThreads = Config.getValue("server_nthreads");
        if(queueSize == null || queueSize.isEmpty()) queueSize = "100";
        if(nThreads == null || nThreads.isEmpty()) nThreads = "2";

        FlashBackServer.port = Integer.valueOf(Config.getValue("server_port"));
        FlashBackServer.queueSize = Integer.valueOf(queueSize);
        FlashBackServer.nThreads = Integer.valueOf(nThreads);

        queue = new ArrayBlockingQueue<RequestResponsePair>(FlashBackServer.queueSize);
        httpServer = new Server(port);


        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new FlashbackHttpServlet(queue), "/*");
        httpServer.setHandler(context);

        for(int i = 0; i < FlashBackServer.nThreads; ++i) {
            Thread consumer = new Thread(new RootDispatcher(queue));
            threads.add(consumer);
            consumer.start();
        }

        httpServer.start();
    }

    public static void stop() throws Exception{
        if(httpServer != null) return;

        httpServer.stop();
        for(var thread : threads) {
            // Do something to thread to stop it
        }
    }
}
