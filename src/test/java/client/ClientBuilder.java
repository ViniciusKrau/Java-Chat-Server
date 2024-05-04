package client;

import java.io.IOException;

public class ClientBuilder {
    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("java", "client.Client");
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}