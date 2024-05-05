package server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {

    static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static ServerSocket serverSocket;
    private static ThreadPoolExecutor executorService;
    static PrivateKey serverPrivateKey;
    static PublicKey serverPublicKey;
    private static KeyPairGenerator keyPairGenerator;

    public static void main(String[] args) {
        

        try {
            Server.keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.err.println("Failed to create KeyPairGenerator instance.");
            return;
        }
        Server.keyPairGenerator.initialize(2048);

        // Generate a public-private key pair
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Get the public and private keys
        serverPrivateKey = keyPair.getPrivate();
        serverPublicKey = keyPair.getPublic();
        

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

               // Read the key from the client
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String publicKeyStringClient = in.readLine();
                X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyStringClient.getBytes()));
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey clientPublicKey = keyFactory.generatePublic(spec);
                // System.out.println(clientPublicKey);
            
                System.out.println("Sending server public key to client");
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println(Base64.getEncoder().encodeToString(serverPublicKey.getEncoded()));

                // Create a new thread to handle the client
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientPublicKey);
                clients.add(clientHandler);
                executorService.execute(clientHandler);
            }


        } catch (RejectedExecutionException e) {
            System.out.println("Task submission rejected. " + e.getMessage());
            throw e;
        } catch (SocketException e) {
            // restartServer();
        }
        catch (Exception e) {
            e.printStackTrace();
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

    public static List<ClientHandler> getClients() {
        return clients;
    }
}