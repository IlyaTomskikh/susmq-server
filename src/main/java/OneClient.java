import susMQ.SusQueue;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class OneClient extends Thread implements Runnable {
    private static final List<OneClient> consumers = new LinkedList<>();
    private static final List<OneClient> producers = new LinkedList<>();
    private final Socket socket;
    private final Logger logger = Logger.getLogger(Thread.currentThread().getName());
    private static SusQueue<String> queue;
    private final AtomicBoolean ready = new AtomicBoolean();
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private static final AtomicInteger consumerCounter = new AtomicInteger();

    private static void send() {
        var cons = OneClient.consumers;
        if (!cons.isEmpty()) {
            while (!queue.isEmpty()) {
                var cl = cons.get(OneClient.consumerCounter.getAndIncrement() % cons.size());
                try {
                    if (!cl.isReady()) continue;
                    cl.dos.writeUTF(queue.poll());
                    cl.dos.flush();
                } catch (IOException e) {
                    cl.logger.info("Couldn't send the message");
                }
            }
        }
    }

    public OneClient(Socket socket, SusQueue<String> queue, char clientType) {
        if (clientType != 'c' && clientType != 'p') {
            logger.info("Incorrect type");
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

    public static void addConsumer(OneClient consumer) {
        OneClient.consumers.add(consumer);
    }

    public static void addProducer(OneClient producer) {
        OneClient.producers.add(producer);
    }

    @Override
    public void run() {
        if (this.socket == null || this.dis == null || this.socket.isClosed()) {
            this.logger.info("All resources are closed");
            return;
        }
        try {
            while (!this.socket.isClosed()) {
                var msg = this.dis.readUTF();
                this.logger.info("Server got: " + msg);
                OneClient.queue.add(msg);
                send();
                Thread.sleep(10);
            }
        } catch (IOException | InterruptedException e) {
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

    private boolean isReady() {
        return this.ready.get();
    }
}
