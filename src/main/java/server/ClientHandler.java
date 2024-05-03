package server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.crypto.SecretKey;


public class ClientHandler extends Thread {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private SecretKey secretKey;

    public ClientHandler(Socket clientSocket, SecretKey secretKey) {
        this.clientSocket = clientSocket;
        this.secretKey = secretKey;
    }

    public void run() {
        try {
            // Create input and output streams for the client
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                // Broadcast the received message to all connected clients
                broadcastMessage(inputLine);
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

    private void broadcastMessage(String message) {
        for (ClientHandler client : Server.clients) {
            if (client != this) { // don't send the message to the sender
                client.writer.println(message);
            }
        }
    }
}