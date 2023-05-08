import susMQ.SusQueue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Application {
    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(2);
        var logger = Logger.getLogger("Main server logger");
        var clients = new LinkedList<OneClient>();
        var capacity = 3;
        var queue = new SusQueue<String>(capacity);
        var producersPort = 1234;
        var consumersPort = 4321;
        service.execute(() -> {
            while (true) {
                try (var serverSocket = new ServerSocket(producersPort,0)) {
                    logger.info("Waiting for producers to connect");
                    var clientSocket = serverSocket.accept();
                    var producer = new OneClient(clientSocket, queue, 'p');
                    OneClient.addProducer(producer);
                    producer.start();
                    clients.add(producer);
                } catch (IOException e) {
                    logger.info("I/O exception occurred");
                }
            }
        });
//        service.execute(() -> {
//            while (true) {
//                try (var serverSocket = new ServerSocket(consumersPort)) {
//                    logger.info("Waiting for consumers to connect");
//                    var client = serverSocket.accept();
//                    var consumer = new OneClient(client, queue, 'c');
//                    OneClient.addConsumer(consumer);
//                    consumer.start();
//                    clients.add(consumer);
//                } catch (IOException e) {
//                    logger.info("I/O exception occurred");
//                }
//            }
//        });
    }
}
