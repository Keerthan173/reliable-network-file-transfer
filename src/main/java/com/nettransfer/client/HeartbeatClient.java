
package com.nettransfer.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class HeartbeatClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5001;
    private static final int INTERVAL_MS = 3000;

    public static void main(String[] args) {

        try (DatagramSocket socket = new DatagramSocket()) {

            InetAddress serverAddress =
                    InetAddress.getByName(SERVER_HOST);

            System.out.println("Heartbeat client started.");
            System.out.println("Sending heartbeat every 3 seconds.");
            System.out.println("Press Ctrl+C to stop.");

            while (true) {

                String message = "HEARTBEAT";

                byte[] data =
                        message.getBytes(StandardCharsets.UTF_8);

                DatagramPacket packet = new DatagramPacket(
                        data,
                        data.length,
                        serverAddress,
                        SERVER_PORT
                );

                socket.send(packet);

                System.out.println("Heartbeat sent: " + message);

                Thread.sleep(INTERVAL_MS);
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            System.out.println("Heartbeat client interrupted.");

        } catch (IOException e) {

            System.out.println(
                    "Heartbeat client error: " + e.getMessage()
            );
        }
    }
}
