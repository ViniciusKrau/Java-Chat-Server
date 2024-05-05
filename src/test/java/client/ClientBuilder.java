package client;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class ClientBuilder {
    public static void main(String[] args) throws InterruptedException {
        // System.getProperty("java.class.path");
        for (int i = 0; i < 50; i++) {

            try {
                ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "client.Client");
                Process process = processBuilder.start();

                OutputStream stdin = process.getOutputStream();
                PrintWriter writer = new PrintWriter(stdin);
                
                writer.write("Process Number: " + i + "\n");
                writer.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("All processes started.");
    }
}