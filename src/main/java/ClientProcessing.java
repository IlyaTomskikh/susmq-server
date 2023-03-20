import susMQ.SusQueue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ClientProcessing extends Thread implements Runnable {
    private final Socket socket;
    private final List<ClientProcessing> clients;
    private PrintWriter out;
    private final Logger logger = Logger.getLogger(Thread.currentThread().getName());
    private final SusQueue<String> queue;
    private int partitionLen;

    private int startPartIx;

    public ClientProcessing(Socket socket, List<ClientProcessing> clients, SusQueue<String> queue) {
        this.socket = socket;
        this.clients = clients;
        this.queue = queue;
    }

    public void resize() {
        if (this.clients.isEmpty()) this.partitionLen = this.queue.getCapacity();
        else {
            this.clients.forEach(c -> c.partitionLen = this.queue.getCapacity() / this.clients.size());
            var d = this.queue.getCapacity() % this.clients.size();
            if (d != 0) this.partitionLen += d;
        }
        if (!this.clients.isEmpty()) {
            var tmp = new AtomicInteger(this.clients.get(0).partitionLen);
            this.clients.forEach(c -> c.startPartIx += tmp.getAndAdd(c.partitionLen));
        }
    }

    @Override
    public void run() {
        this.resize();
        try {
            var in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            var num = 0;
            while (true) {
                var outStr = in.readLine();
                if (outStr == null || outStr.equalsIgnoreCase("exit")) break;
                this.queue.add(outStr);
                send(this.queue.take(), num);
                ++num;
                if (num < 0) num ^= num;
                num %= this.clients.size();
                this.logger.info("Server got " + outStr);
            }
        } catch (IOException e) {
            this.logger.info("I/O exception occurred on 'try{...}' in 'run()'");
        } finally {
            this.out.close();
            try {
                this.socket.close();
            } catch (IOException e) {
                this.logger.info("I/O exception occurred while closing");
            }
            this.clients.remove(this);
            this.interrupt();
        }
    }

    private void send(String outStr, int num) {
        this.clients.get(num).out.println(outStr);
    }
}
