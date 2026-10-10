
package com.nettransfer.server;

import com.nettransfer.util.FileIntegrity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private volatile long totalBytesForStats = 0;
    private String transferStatus = "FAILED";

    // Keep the output file accessible to the catch block
    private File outputFile;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

        long startTime = System.currentTimeMillis();

        System.out.println(
                "[" + LocalDateTime.now()
                        + "] Transfer started on thread: "
                        + Thread.currentThread().getName()
        );

        try (Socket socket = clientSocket;
             DataInputStream inputStream =
                     new DataInputStream(socket.getInputStream());
             DataOutputStream outputStream =
                     new DataOutputStream(socket.getOutputStream())) {

            // Step 1: Read metadata from the client
            String fileName = inputStream.readUTF();
            long fileSize = inputStream.readLong();
            String expectedHash = inputStream.readUTF();

            System.out.println("Filename received: " + fileName);
            System.out.println("File size: " + fileSize + " bytes");

            // Step 2: Validate metadata
            if (fileSize < 0) {
                System.out.println("Invalid file size.");
                transferStatus = "FAILED";
                outputStream.writeUTF("FAILED");
                outputStream.flush();
                return;
            }

            // Step 3: Create a safe destination filename
            String safeFileName = new File(fileName).getName();
            outputFile = new File("received_" + safeFileName);

            // Step 4: Receive the declared number of bytes
            long totalBytes = 0;
            byte[] buffer = new byte[8192];

            try (FileOutputStream fileOutputStream =
                         new FileOutputStream(outputFile)) {

                while (totalBytes < fileSize) {

                    int bytesToRead = (int) Math.min(
                            buffer.length,
                            fileSize - totalBytes
                    );

                    int bytesRead = inputStream.read(
                            buffer, 0, bytesToRead
                    );

                    if (bytesRead == -1) {
                        break;
                    }

                    fileOutputStream.write(buffer, 0, bytesRead);

                    totalBytes += bytesRead;
                    totalBytesForStats = totalBytes;
                }
            }

            System.out.println(
                    "Total bytes received: " + totalBytes
            );

            // Step 5: Reject incomplete transfers
            if (totalBytes != fileSize) {

                System.out.println(
                        "File transfer incomplete. Expected: "
                                + fileSize + ", received: " + totalBytes
                );

                deletePartialFile();

                transferStatus = "FAILED";
                outputStream.writeUTF("FAILED");
                outputStream.flush();
                return;
            }

            System.out.println("File transfer completed.");
            System.out.println(
                    "Saved file: " + outputFile.getAbsolutePath()
            );

            // Step 6: Calculate the received file's hash
            String receivedHash =
                    FileIntegrity.calculateSHA256(outputFile);

            System.out.println("Expected SHA-256: " + expectedHash);
            System.out.println("Received SHA-256: " + receivedHash);

            // Step 7: Verify integrity and acknowledge the client
            if (expectedHash.equalsIgnoreCase(receivedHash)) {

                transferStatus = "SUCCESS";

                System.out.println(
                        "INTEGRITY OK: File contents match."
                );

                outputStream.writeUTF("SUCCESS");
                outputStream.flush();

                System.out.println("ACK sent: SUCCESS");

            } else {

                transferStatus = "FAILED";

                System.out.println(
                        "INTEGRITY FAILED: File contents do not match!"
                );

                // Remove a file that failed integrity verification
                deletePartialFile();

                outputStream.writeUTF("FAILED");
                outputStream.flush();

                System.out.println("ACK sent: FAILED");
            }

        } catch (NoSuchAlgorithmException e) {

            transferStatus = "FAILED";

            System.out.println(
                    "SHA-256 algorithm unavailable: " + e.getMessage()
            );

            deletePartialFile();

        } catch (IOException e) {

            transferStatus = "FAILED";

            System.out.println(
                    "File transfer failed: " + e.getMessage()
            );

            // Handles abrupt disconnections such as Connection reset
            deletePartialFile();

        } finally {

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            double speedMBps = 0.0;

            if (duration > 0) {
                double sizeInMiB =
                        totalBytesForStats / (1024.0 * 1024.0);

                double durationInSeconds = duration / 1000.0;

                speedMBps = sizeInMiB / durationInSeconds;
            }

            System.out.println(
                    "[" + LocalDateTime.now()
                            + "] Transfer finished on thread: "
                            + Thread.currentThread().getName()
            );

            System.out.println(
                    "\n========== TRANSFER SUMMARY =========="
            );
            System.out.println(
                    "Client: " + clientSocket.getInetAddress()
            );
            System.out.println(
                    "Thread: " + Thread.currentThread().getName()
            );
            System.out.println(
                    "Bytes received: " + totalBytesForStats
            );
            System.out.println("Duration: " + duration + " ms");
            System.out.printf(
                    "Processing rate: %.2f MiB/s%n",
                    speedMBps
            );
            System.out.println("Status: " + transferStatus);
            System.out.println("======================================");

            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println(
                        "Could not close client connection."
                );
            }
        }
    }

    // Delete an incomplete or integrity-failed file
    private void deletePartialFile() {

        if (outputFile != null && outputFile.exists()) {

            if (outputFile.delete()) {
                System.out.println(
                        "Partial or invalid file deleted: "
                                + outputFile.getName()
                );
            } else {
                System.out.println(
                        "Could not delete partial or invalid file: "
                                + outputFile.getName()
                );
            }
        }
    }
}
