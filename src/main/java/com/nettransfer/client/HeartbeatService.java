
package com.nettransfer.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class HeartbeatService {

    private static final int SERVER_PORT = 5001;
    private static final long INTERVAL_MS = 3000;

    private final String clientId;
    private final String serverHost;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private DatagramSocket socket;
    private Thread heartbeatThread;

    public HeartbeatService(String clientId, String serverHost) {
        this.clientId = clientId;
        this.serverHost = serverHost;
    }

    public void start() throws IOException {

        if (!running.compareAndSet(false, true)) {
            return;
        }

        socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(serverHost);

        heartbeatThread = new Thread(() -> {
            try {
                while (running.get()) {

                    String message = "HEARTBEAT:" + clientId;
                    byte[] data =
                            message.getBytes(StandardCharsets.UTF_8);

                    DatagramPacket packet = new DatagramPacket(
                            data,
                            data.length,
                            serverAddress,
                            SERVER_PORT
                    );

                    socket.send(packet);

                    Thread.sleep(INTERVAL_MS);
                }

            } catch (IOException e) {
                if (running.get()) {
                    System.out.println(
                            "Heartbeat error: " + e.getMessage()
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "heartbeat-sender-" + clientId);

        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        System.out.println("Heartbeat service started for " + clientId);
    }

    public void stop() {

        running.set(false);

        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        System.out.println("Heartbeat service stopped for " + clientId);
    }
}
