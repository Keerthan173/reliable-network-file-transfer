package com.nettransfer.client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {

    public static void main(String[] args) {

        try {
            Socket socket = new Socket("localhost", 5000);

            System.out.println("Connected to server!");

            DataOutputStream outputStream =
                    new DataOutputStream(socket.getOutputStream());

            File file = new File("test.txt");

            // Send file name
            outputStream.writeUTF(file.getName());

            // Send file size
            outputStream.writeLong(file.length());

            System.out.println("Filename: " + file.getName());
            System.out.println("File size: " + file.length() + " bytes");

            // Open file
            FileInputStream fileInputStream =
                    new FileInputStream(file);

            byte[] buffer = new byte[8192];

            long totalBytes = 0;
            int bytesRead;

            // Send file data
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {

                outputStream.write(buffer, 0, bytesRead);

                totalBytes += bytesRead;

                System.out.println(
                        "Chunk sent: " + bytesRead + " bytes"
                );
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