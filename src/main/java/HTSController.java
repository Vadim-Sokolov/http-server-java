import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HTSController {

    private final HTSService service;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public HTSController() {
        this.service = new HTSService();
    }

    public void run() {
        running = true;

        try {
            serverSocket = new ServerSocket(4221);
            serverSocket.setReuseAddress(true);
            System.out.println("Server started. Awaiting connection");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                    Thread clientHandler = new Thread(() -> {
                        try {
                            service.handleRequest(clientSocket);
                        } finally {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                System.out.println("Error closing client socket: " + e.getMessage());
                            }
                        }
                    });
                    clientHandler.start();
                } catch (IOException e) {
                    if (!running) {
                        System.out.println("Server has been stopped.");
                    } else {
                        System.out.println("Connection processing error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        } finally {
            closeServerSocket();
        }
    }

    public void stopServer() {
        running = false;
        closeServerSocket();
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket closed.");
            } catch (IOException e) {
                System.out.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}
