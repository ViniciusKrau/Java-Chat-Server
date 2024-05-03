package client;

import java.io.IOException;
import java.net.Socket;

public class ClientBuilder {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;

    public static void main(String[] args) {
        int n = 5; // Number of client threads to create

        for (int i = 0; i < n; i++) {
            Thread clientThread = new Thread(() -> {
                try {
                    Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                    // Perform client operations here
                    // ...
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            clientThread.start();
        }
    }
}