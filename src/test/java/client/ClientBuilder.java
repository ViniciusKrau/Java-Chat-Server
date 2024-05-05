package client;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ClientBuilder {
    public static void main(String[] args) throws InterruptedException {
        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < 50; i++) {

            try {
                ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "client.Client");
                Process process = processBuilder.start();
                processes.add(process);

                OutputStream stdin = process.getOutputStream();
                PrintWriter writer = new PrintWriter(stdin);

                writer.write("Process Number: " + i + "\n");
                writer.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("All processes started.");

        for (Process process : processes) {
            process.destroy();
        }
        System.out.println("All processes closed.");

    }
}