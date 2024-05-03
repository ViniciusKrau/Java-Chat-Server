package server;


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {

    public static List<ClientHandler> clients = new ArrayList<>();
    public static void main(String[] args) {

        int corePoolSize = 10;
        int maximumPoolSize = 20;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(50); // queue with a capacity of 50
        
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            workQueue
        );

        try {
            // Create a server socket
            InetAddress localhost = InetAddress.getByName("127.0.0.1");
            ServerSocket serverSocket = new ServerSocket(1234, 50, localhost);
            System.out.println("Server started. Waiting for clients to connect...");

            while (true) {
                // Accept client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Create a new thread to handle the client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Server.clients.add(clientHandler);
                executorService.execute(clientHandler);
                // clientHandler.start();
            }
        } catch (RejectedExecutionException e) {
            System.out.println("Task submission rejected. " + e.getMessage());
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}