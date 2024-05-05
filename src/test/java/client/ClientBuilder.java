package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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

                // Write to the process's input stream
                // Thread.sleep(300);
                writer.write("Process Number: " + i + "\n");
                writer.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}