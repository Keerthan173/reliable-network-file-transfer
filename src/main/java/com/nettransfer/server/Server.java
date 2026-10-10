package com.nettransfer.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {

        Thread heartbeatThread = new Thread(
                HeartbeatServer::start,
                "udp-heartbeat-server"
        );

        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        try (ServerSocket serverSocket = new ServerSocket(5000)) {

            System.out.println("Server started on port 5000.");

            while (true) {
                System.out.println("Waiting for client...");

                Socket clientSocket = serverSocket.accept();

                System.out.println(
                        "[" + java.time.LocalDateTime.now()
                                + "] Client connected from: "
                                + clientSocket.getInetAddress()
                );


                ClientHandler clientHandler =
                        new ClientHandler(clientSocket);

                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}