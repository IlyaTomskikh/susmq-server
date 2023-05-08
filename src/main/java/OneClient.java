import susMQ.SusQueue;

import java.io.*;
import java.net.Socket;
import java.util.*;
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
            while (!this.socket.isClosed() && !this.socket.isOutputShutdown() && this.socket.isConnected()) {
                var msg = this.dis.readUTF();
                this.logger.info("Server got: " + msg);
                OneClient.queue.add(msg);
                send();
                Thread.sleep(10);
            }
        } catch (EOFException e) {
            logger.info("Client has disconnected");
//            try {
//                this.socket.close();
//            } catch (IOException ex) {
//                logger.info("Couldn't close the connection");
//            }
        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
            this.logger.info("Exception occurred on 'try{...}' in 'run()'");
        } finally {
            producers.get(producers.indexOf(this)).close();
        }
    }

    private boolean isReady() {
        return this.ready.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OneClient oneClient)) return false;

        if (!Objects.equals(socket, oneClient.socket)) return false;
        if (!logger.equals(oneClient.logger)) return false;
        if (!ready.equals(oneClient.ready)) return false;
        if (!Objects.equals(dis, oneClient.dis)) return false;
        return Objects.equals(dos, oneClient.dos);
    }

    @Override
    public int hashCode() {
        int result = socket != null ? socket.hashCode() : 0;
        result = 31 * result + logger.hashCode();
        result = 31 * result + ready.hashCode();
        result = 31 * result + (dis != null ? dis.hashCode() : 0);
        result = 31 * result + (dos != null ? dos.hashCode() : 0);
        return result;
    }

    private void close() {
        try {
            this.socket.close();
            this.dis.close();
            this.dos.close();
        } catch (IOException e) {
            logger.info("Connections are already closed");
        }
    }
}
