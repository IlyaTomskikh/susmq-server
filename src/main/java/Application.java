import susMQ.SusQueue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.logging.Logger;

public class Application {
    public static void main(String[] args) {
        var logger = Logger.getLogger("Main server logger");
        var clients = new LinkedList<ClientProcessing>();
        var capacity = 3;
        var queue = new SusQueue<String>(capacity);
        var port = 5000;
        var timeout = 0;
        try(var serverSocket = new ServerSocket(port, timeout)) {
            while (true) {
                var socket = serverSocket.accept();
                clients.add(new ClientProcessing(socket, clients, queue));
                clients.getLast().start();
            }
        } catch (IOException e) {
            logger.info("I/O exception occurred");
        }
    }
}
