import susMQ.SusQueue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

public class OneClient extends Thread implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private final Logger logger = Logger.getLogger(Thread.currentThread().getName());
    private final SusQueue<String> queue;
    private boolean ready;

    public OneClient(Socket socket, SusQueue<String> queue) {
        this.socket = socket;
        this.queue = queue;
        this.ready = true;
    }

    @Override
    public void run() {
        try {
            var in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            while (true) {
                var outStr = in.readLine();
                if (outStr == null || outStr.equalsIgnoreCase("exit")) break;
                this.queue.add(outStr);
                this.out.println(outStr);
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
            this.interrupt();
        }
    }
}
