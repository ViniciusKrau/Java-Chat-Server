package client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;



public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1234;

    static PublicKey serverPublicKey;
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private static KeyPairGenerator keyPairGenerator;
    private static Socket socket;

    public static void main(String[] args) {

        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.err.println("Failed to create KeyPairGenerator instance.");
            return;
        }
        keyPairGenerator.initialize(2048);

        // Generate a public-private key pair
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Get the public and private keys
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        boolean connected = false;
        while (!connected) {
            try {
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                System.out.println("Connected to server.");
                connected = true;
            } catch (ConnectException e) {
                System.out.println("Failed to connect to server. Retrying...");
                try {
                    // Wait for a while before retrying to avoid busy-waiting
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("An IO error occurred while trying to connect to the server.");
                break;
            }
        }
        try {

            BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter write = new PrintWriter(socket.getOutputStream(), true);

            // Send the client's public key to the server
            write.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));

            // Receive the server's public key
            String publicKeyString = read.readLine();
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString.getBytes());
            
            X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            serverPublicKey = keyFactory.generatePublic(spec);



            // Start a separate thread to listen for incoming messages from the server
            Thread messageListener = new Thread(() -> {
                try {
                    String message;
                    while ((message = read.readLine()) != null) {
                        // System.out.println("Encrypted message: " + message);
                        System.out.println("Received message: " + decryptMessage(message));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            messageListener.start();

            // Read messages from the console and send them to the server
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
            String message;
            while ((message = consoleInput.readLine()) != null) {
                write.println(encryptMessage(message));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    e.printStackTrace();
                }
            }
        }
    }

    private static String encryptMessage(String message) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey); 
            byte[] encryptedBytes = cipher.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting message", e);
        }
    }

    private static String decryptMessage(String encryptedMessage) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedMessage = new String(decryptedBytes);
            return decryptedMessage;
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting message", e);
        }
    }
}