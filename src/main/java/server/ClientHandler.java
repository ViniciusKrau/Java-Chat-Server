package server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;




public class ClientHandler extends Thread {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;

    private PublicKey publicKey;

    public ClientHandler(Socket clientSocket, PublicKey publicKey) {
        this.clientSocket = clientSocket;
        this.publicKey = publicKey;
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        String inputLine;
        try {
            while ((inputLine = reader.readLine()) != null) {
                // Broadcast the received message to all connected clients
                String decryptedMessage = decryptMessage(inputLine);
                broadcastMessage(decryptedMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the client socket
                Server.clients.remove(this);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String encryptMessage(String message, PublicKey publicKey) {
        // System.out.println("Public key: " + publicKey.toString());
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes("UTF-8"));
            // System.out.println("Encrypted message: " + Base64.getEncoder().encodeToString(encryptedBytes));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting message", e);
        }
    }

    private String decryptMessage(String encryptedMessage) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, Server.serverPrivateKey);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedMessage = new String(decryptedBytes);
            // System.out.println("Decrypted message: " + decryptedMessage);
            return decryptedMessage;
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting message", e);
        }
    }

    
    private void broadcastMessage(String message) {
        for (ClientHandler client : Server.clients) {
            // System.out.println(client.getPublicKey());
            if (client != this) { // don't send the message to the sender
                client.encryptMessage(message, client.getPublicKey());
                client.writer.println(encryptMessage(message, client.getPublicKey()));
            }
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}