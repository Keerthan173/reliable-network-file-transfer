
package com.nettransfer.client;

import com.nettransfer.util.FileIntegrity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class Client {

    public static void main(String[] args) {

        // Step 1: Check whether a filename was provided
        if (args.length == 0) {
            System.out.println("Usage: Client <filename>");
            return;
        }

        // Step 2: Validate the file
        File file = new File(args[0]);

        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + file.getPath());
            return;
        }

        // Stable identity for this client process
        String clientId = UUID.randomUUID().toString();
        HeartbeatService heartbeatService =
                new HeartbeatService(clientId, "localhost");

        // Step 3: Connect to the TCP server
        try (Socket socket = new Socket("localhost", 5000)) {

            socket.setSoTimeout(30000);

            System.out.println("Connected to server!");
            System.out.println("Client ID: " + clientId);

            // Step 4: Start UDP heartbeats
            heartbeatService.start();

            // Step 5: Calculate the original SHA-256 hash
            String originalHash = FileIntegrity.calculateSHA256(file);

            System.out.println("Original SHA-256: " + originalHash);

            // Keep socket streams open until ACK is received
            try (DataOutputStream outputStream =
                         new DataOutputStream(socket.getOutputStream());
                 DataInputStream inputStream =
                         new DataInputStream(socket.getInputStream())) {

                // Step 6: Send metadata and file contents
                try (FileInputStream fileInputStream =
                             new FileInputStream(file)) {

                    outputStream.writeUTF(file.getName());
                    outputStream.writeLong(file.length());
                    outputStream.writeUTF(originalHash);

                    System.out.println("Filename: " + file.getName());
                    System.out.println(
                            "File size: " + file.length() + " bytes"
                    );

                    byte[] buffer = new byte[8192];
                    long totalBytes = 0;
                    int bytesRead;

                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                        totalBytes += bytesRead;

                        // Temporary delay for interruption testing
                        Thread.sleep(100);
                    }

                    System.out.println("Total bytes sent: " + totalBytes);
                    System.out.println("Waiting for server ACK...");
                }

                // Step 7: Read the server acknowledgment
                String acknowledgment = inputStream.readUTF();

                if ("SUCCESS".equals(acknowledgment)) {
                    System.out.println(
                            "Server ACK: File received and verified successfully."
                    );
                } else if ("FAILED".equals(acknowledgment)) {
                    System.out.println(
                            "Server ACK: File transfer or integrity verification failed."
                    );
                } else {
                    System.out.println(
                            "Unexpected server acknowledgment: "
                                    + acknowledgment
                    );
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            System.out.println("File transfer interrupted.");

        } catch (SocketTimeoutException e) {

            System.out.println(
                    "Timeout: Server did not respond within 30 seconds."
            );

        } catch (NoSuchAlgorithmException e) {

            System.out.println(
                    "SHA-256 algorithm unavailable: " + e.getMessage()
            );

        } catch (IOException e) {

            System.out.println(
                    "File transfer failed: " + e.getMessage()
            );

        } finally {

            // Stop heartbeats when the client finishes or fails
            heartbeatService.stop();
        }
    }
}