import susMQ.SusQueue;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class OneClient extends Thread implements Runnable {
    private static List<OneClient> consumers;
    private static List<OneClient> producers;
    private final Socket socket;
    private final Logger logger = Logger.getLogger(Thread.currentThread().getName());
    private static SusQueue<String> queue;
    private final AtomicBoolean ready = new AtomicBoolean();
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private static AtomicInteger consumerCounter = new AtomicInteger();

    private static void send(String msg) {
        if (!OneClient.consumers.isEmpty()) {
            var cl = OneClient.consumers.get(OneClient.consumerCounter.getAndIncrement() % OneClient.consumers.size());
            try {
                cl.dos.writeUTF(msg);
                cl.dos.flush();
            } catch (IOException e) {
                cl.logger.info("Couldn't send the message");
                throw new RuntimeException(e);
            }
        }
    }

    public OneClient(Socket socket, SusQueue<String> queue, char clientType) {
        if (clientType != 'c' || clientType != 'p') {
            this.socket = null;
            this.dis = null;
            this.dos = null;
            return;
        }
        DataInputStream dis1;
        DataOutputStream dos1;
        this.socket = socket;
        OneClient.queue = queue;
        this.ready.set(true);
        try {
            dis1 = new DataInputStream(socket.getInputStream());
            dos1 = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            this.ready.set(false);
            dis1 = null;
            dos1 = null;
            logger.info("Couldn't connect the client");
        }
        this.dis = dis1;
        this.dos = dos1;
        if (clientType == 'c') OneClient.consumers.add(this);
        else if (clientType == 'p') OneClient.producers.add(this);
    }

    @Override
    public void run() {
        try {
            while (!this.socket.isClosed()) {
                var msg = this.dis.readUTF();
                this.logger.info("Server got: " + msg);
                send(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
//            this.logger.info("I/O exception occurred on 'try{...}' in 'run()'");
        } /*finally {
            this.out.close();
            try {
                this.socket.close();
            } catch (IOException e) {
                this.logger.info("I/O exception occurred while closing");
            }
            this.interrupt();
        }*/
    }
}
