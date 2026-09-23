package com.nettransfer.client;

import java.io.*;
import java.net.Socket;

public class Client {

    public static void main(String[] args) {

        try {
            Socket socket = new Socket("localhost", 5000);

            System.out.println("Connected to server!");

            FileInputStream fileInputStream =
                    new FileInputStream("test.txt");
            byte[] buffer = new byte[8192];

            OutputStream outputStream = socket.getOutputStream();

            int totalBytes = 0;
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                System.out.println("Chunk sent: " + bytesRead + " bytes");
            }

            outputStream.flush();

            System.out.println("Total bytes sent: " + totalBytes);
            fileInputStream.close();


            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}