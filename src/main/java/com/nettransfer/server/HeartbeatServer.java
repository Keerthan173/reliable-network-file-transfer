
package com.nettransfer.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HeartbeatServer {

    private static final int PORT = 5001;
    private static final int BUFFER_SIZE = 1024;
    private static final long TIMEOUT_MS = 8000;

    private static final Map<String, Long> lastHeartbeat =
            new ConcurrentHashMap<>();

    private static final Map<String, String> clientStatus =
            new ConcurrentHashMap<>();

    public static void start() {

        try (DatagramSocket socket = new DatagramSocket(PORT)) {

            System.out.println(
                    "UDP Heartbeat Server started on port " + PORT
            );

            Thread monitorThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(2000);
                        checkInactiveClients();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "heartbeat-monitor");

            monitorThread.setDaemon(true);
            monitorThread.start();

            while (true) {

                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);

                String message = new String(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength(),
                        StandardCharsets.UTF_8
                ).trim();

                // Expect HEARTBEAT:<clientId>
                if (!message.startsWith("HEARTBEAT:")) {
                    continue;
                }

                String clientId =
                        message.substring("HEARTBEAT:".length()).trim();

                if (clientId.isEmpty()) {
                    continue;
                }

                long currentTime = System.currentTimeMillis();
                lastHeartbeat.put(clientId, currentTime);

                String previousStatus =
                        clientStatus.put(clientId, "ONLINE");

                if (previousStatus == null) {

                    System.out.println(
                            "[" + LocalTime.now() + "] "
                                    + clientId + " - CLIENT ONLINE"
                    );

                } else if ("INACTIVE".equals(previousStatus)) {

                    System.out.println(
                            "[" + LocalTime.now() + "] "
                                    + clientId + " - CLIENT ONLINE AGAIN"
                    );
                }
            }

        } catch (SocketException e) {

            System.out.println(
                    "UDP server error: " + e.getMessage()
            );

        } catch (IOException e) {

            System.out.println(
                    "Heartbeat server error: " + e.getMessage()
            );
        }
    }

    private static void checkInactiveClients() {

        long currentTime = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry :
                lastHeartbeat.entrySet()) {

            String clientId = entry.getKey();
            long lastSeen = entry.getValue();

            if (currentTime - lastSeen > TIMEOUT_MS) {

                boolean changed = clientStatus.replace(
                        clientId, "ONLINE", "INACTIVE"
                );

                if (changed) {
                    System.out.println(
                            "[" + LocalTime.now() + "] "
                                    + clientId + " - CLIENT INACTIVE"
                    );
                }
            }
        }
    }
}
