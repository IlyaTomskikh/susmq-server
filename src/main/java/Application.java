import susMQ.SusQueue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.logging.Logger;

public class Application {
    public static void main(String[] args) {
        var logger = Logger.getLogger("Main server logger");
        var clients = new LinkedList<OneClient>();
        var capacity = 3;
        var queue = new SusQueue<String>(capacity);
        var producersPort = 1234;
        var consumersPort = 4321;
        try(var serverSocket = new ServerSocket(producersPort)) {
            logger.info("Server is ready");
            var client = serverSocket.accept();
            clients.add(new OneClient(client, queue, 'c'));
        } catch (IOException e) {
            logger.info("I/O exception occurred");
        }
    }
}
