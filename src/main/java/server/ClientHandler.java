package server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;

import lombok.Getter;


public class ClientHandler extends Thread {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    @Getter
    private PublicKey publicKey;

    public ClientHandler(Socket clientSocket, PublicKey publicKey, BufferedReader reader, PrintWriter writer) {
        this.clientSocket = clientSocket;
        this.publicKey = publicKey;
        this.reader = reader;
        this.writer = writer;
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

    private String encryptMessage(String message) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, this.publicKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes("UTF-8"));
            System.out.println(decryptMessage(Base64.getEncoder().encodeToString(encryptedBytes)));
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
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting message", e);
        }
    }

    
    private void broadcastMessage(String message) {
        for (ClientHandler client : Server.clients) {
            if (client != this) { // don't send the message to the sender
                client.encryptMessage(message);
                client.writer.println(message);
            }
        }
    }
}