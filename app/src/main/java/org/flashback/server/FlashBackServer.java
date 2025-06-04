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

    private static final BlockingQueue<RequestResponsePair> queue = new ArrayBlockingQueue<>(queueSize);
    private static Server httpServer = new Server(port);
    private static final ArrayList<Thread> threads = new ArrayList<>();

    public static void start() throws Exception{
        FlashBackServer.port = Integer.valueOf(Config.getValue("server_port"));

        String queueSize = Config.getValue("server_queue_size");
        String nThreads = Config.getValue("server_nthreads");
        if(queueSize == null || queueSize.isEmpty()) queueSize = "100";
        if(nThreads == null || nThreads.isEmpty()) nThreads = "2";

        FlashBackServer.queueSize = Integer.valueOf(queueSize);
        FlashBackServer.nThreads = Integer.valueOf(nThreads);


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
