package org.flashback.server;

import org.flashback.auth.Authenticator;
import org.flashback.helpers.Config;
import org.flashback.helpers.NoteProcessor;
import org.flashback.types.RequestResponsePair;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;

public class FlashBackServer {
    private static FlashBackServer server;

    private int port;
    private int nThreads;
    private int queueSize;

    private final BlockingQueue<RequestResponsePair> queue = new ArrayBlockingQueue<>(queueSize);
    private Server httpServer = new Server(port);
    private final ArrayList<Thread> threads = new ArrayList<>();

    public static void init() throws Exception {
        if(server == null) {
            Authenticator.loadConfig();
            NoteProcessor.init();
            server = new FlashBackServer();
        }
    }

    public static FlashBackServer getServer() throws Exception {
        if(server == null) {
            throw new Exception("server is null");
        }

        return server;
    }

    private FlashBackServer() throws Exception {
        this.port = Integer.valueOf(Config.getValue("server_port"));

        String queueSize = Config.getValue("server_queue_size");
        String nThreads = Config.getValue("server_nthreads");
        if(queueSize == null || queueSize.isEmpty()) queueSize = "100";
        if(nThreads == null || nThreads.isEmpty()) nThreads = "2";

        this.queueSize = Integer.valueOf(Config.getValue("server_queue_size"));
        this.nThreads = Integer.valueOf(Config.getValue("server_nthreads"));
    };

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
        for(var thread : threads) {
            // Do something to thread to stop it
        }
    }
}
