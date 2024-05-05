package server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
            while (clientSocket.isConnected() && !clientSocket.isClosed() && (inputLine = reader.readLine()) != null) {
                // Broadcast the received message to all connected clients
                String decryptedMessage = decryptMessage(inputLine);
                if(decryptedMessage != null){
                    if(decryptedMessage.equals("Restart")){
                        System.out.println("Restarting server...");
                        Server.restartServer();
                    }
                    broadcastMessage(decryptedMessage);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            try {
                // Close the client socket
                Server.clients.remove(this);
                System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
                clientSocket.close();
            } catch (IOException ex) {
                e.printStackTrace();
            }
        }
    }

    private String encryptMessage(String message, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes("UTF-8"));
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
            return decryptedMessage;
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting message", e);
        }
    }

    
    private void broadcastMessage(String message) {
        List<ClientHandler> clientsCopy;
        synchronized (Server.clients) {
            clientsCopy = new ArrayList<>(Server.clients);
        }
        for (ClientHandler client : clientsCopy) {
            if (client != this) {
                client.writer.println(encryptMessage(message, client.getPublicKey()));
            }
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}