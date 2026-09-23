package com.nettransfer.server;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(5000);

            System.out.println("Server started.");
            System.out.println("Waiting for client...");

            Socket clientSocket = serverSocket.accept();

            System.out.println("Client connected!");

            DataInputStream inputStream =
                    new DataInputStream(clientSocket.getInputStream());

            // Receive file name
            String fileName = inputStream.readUTF();

            System.out.println("Filename received: " + fileName);

            // Receive file size
            long fileSize = inputStream.readLong();

            System.out.println("File size: " + fileSize + " bytes");

            // Create the received file
            FileOutputStream fileOutputStream =
                    new FileOutputStream("received_" + fileName);

            byte[] buffer = new byte[8192];

            long totalBytes = 0;
            int bytesRead;

            // Receive exactly the announced file size
            while (totalBytes < fileSize &&
                    (bytesRead = inputStream.read(buffer, 0,
                            (int) Math.min(buffer.length, fileSize - totalBytes))) != -1) {

                fileOutputStream.write(buffer, 0, bytesRead);

                totalBytes += bytesRead;

                System.out.println(
                        "Chunk received: " + bytesRead + " bytes"
                );
            }

            fileOutputStream.close();

            System.out.println("Total bytes received: " + totalBytes);

            if (totalBytes == fileSize) {
                System.out.println("File transfer completed successfully.");
            } else {
                System.out.println("File transfer incomplete.");
            }

            clientSocket.close();
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}