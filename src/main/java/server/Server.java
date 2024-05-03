package server;


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class Server {

    public static List<ClientHandler> clients = new ArrayList<>();
    private static ServerSocket serverSocket;
    private static ThreadPoolExecutor executorService;
    static Map<String, SecretKey> clientKeys = new HashMap<>();
    private static Map<String, Cipher> clientCiphers = new HashMap<>(); 
    static KeyGenerator keyGen;
    
    

    public static void main(String[] args) {

        try {
            Server.keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.err.println("Failed to create KeyGenerator instance.");
            return;
        }
        Server.keyGen.init(128);

        int corePoolSize = 10;
        int maximumPoolSize = 20;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(50); // queue with a capacity of 50

        executorService = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue
        );

        try {
            // Create a server socket
            InetAddress localhost = InetAddress.getByName("127.0.0.1");
            serverSocket = new ServerSocket(1234, 50, localhost);
            System.out.println("Server started. Waiting for clients to connect...");

            while (true) {
                // Accept client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Generate UniqueClientID
                String clientID = Base64.getEncoder().encodeToString(clientSocket.getInetAddress().getAddress());

                //Generate a key for the client
                SecretKey clientkey = generateKey(clientID);

                // Create a new thread to handle the client
                ClientHandler clientHandler = new ClientHandler(clientSocket,clientkey);
                Server.clients.add(clientHandler);
                executorService.execute(clientHandler);
                // clientHandler.start();
            }
        } catch (RejectedExecutionException e) {
            System.out.println("Task submission rejected. " + e.getMessage());
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeServer();
        }
    }

    public static void closeServer() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (executorService != null) {
                executorService.shutdown();
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public static void restartServer() {
        closeServer();
        main(null);
    }

    public static SecretKey generateKey(String clientId) {

        SecretKey secretKey = Server.keyGen.generateKey();
        clientKeys.put(clientId, secretKey);
        return secretKey;
    }
}