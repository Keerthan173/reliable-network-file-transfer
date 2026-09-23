package com.nettransfer.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(5000);

            System.out.println("Server started.");
            System.out.println("Waiting for client...");

            Socket clientSocket = serverSocket.accept();        // waits for a client
            System.out.println("Client connected!");



            InputStream inputStream = clientSocket.getInputStream();

            FileOutputStream fileOutputStream =
                    new FileOutputStream("received.txt");

            byte[] buffer = new byte[8192];

            int totalBytes = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                System.out.println("Chunk received: " + bytesRead + " bytes");
            }
            fileOutputStream.close();
            System.out.println("Total bytes received: " + totalBytes);


            clientSocket.close();
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}