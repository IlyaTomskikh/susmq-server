import susMQ.SusQueue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Application {

    static List<OneClient> clients = new LinkedList<>();
    static int capacity = 3;
    static SusQueue<String> queue = new SusQueue<>(capacity);

    static int producersPort = 1234;
    static int consumersPort = 4321;
    static Runtime currentRuntime = Runtime.getRuntime();
    private static final Logger logger = Logger.getLogger("Main server logger");

    private static void generateProducer() {
        while (true) {
            try (var serverSocket = new ServerSocket(producersPort,0)) {
                logger.info("Waiting for producers to connect");
                var clientSocket = serverSocket.accept();
                var producer = new OneClient(clientSocket, queue, 'p');
                OneClient.addProducer(producer);
                producer.start();
                clients.add(producer);
                logger.info("Free space after successfully connected a producer " + currentRuntime.freeMemory());
            } catch (IOException e) {
                logger.info("I/O exception occurred");
            }
            logger.info("Free space after try-catch for prod " + currentRuntime.freeMemory());
        }
    }

    private static void generateConsumer() {
        while (true) {
            try (var serverSocket = new ServerSocket(consumersPort)) {
                logger.info("Waiting for consumers to connect");
                var client = serverSocket.accept();
                var consumer = new OneClient(client, queue, 'c');
                OneClient.addConsumer(consumer);
                consumer.start();
                clients.add(consumer);
                logger.info("Free space after successfully connected a consumer " + currentRuntime.freeMemory());
            } catch (IOException e) {
                logger.info("I/O exception occurred");
            }
            logger.info("Free space after try-catch for cons " + currentRuntime.freeMemory());
        }
    }

    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(2);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.info("Free space on start " + currentRuntime.freeMemory());
        service.execute(Application::generateProducer);
        service.execute(Application::generateConsumer);
    }
}
