package client;

import common.MessageParser;
import common.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Client {
    private static final Object lock = new Object();
    private final int port;
    private final static String id = UUID.randomUUID().toString().substring(0, 8);

    public Client(int port) {
        this.port = port;
    }

    public void start() {
        new Thread(this::listenForMessages).start();
    }

    private void listenForMessages() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleIncomingMessage(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void handleIncomingMessage(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(":");
                MessageType type = MessageParser.parseType(message);

                switch (type) {
                    case ACK:
                        synchronized (lock) {
                            System.out.println("Value added successfully! (ACK from server " + parts[1] + ")");
                            lock.notify();
                        }
                        break;
                    case NACK:
                        System.out.println("An error occurred! (NACK from server" + parts[2] + ")");
                        break;
                    default:
                        System.out.println("Unknown message type!");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void sendAddClient() {}

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        Scanner scanner = new Scanner(System.in);
        if (args.length != 3) {
            System.out.println("Usage: java Client <port> <peerIp> <peerPort>");
            scanner.close();
            return;
        }
        int port = Integer.parseInt(args[0]);
        String peerIp = args[1];
        int peerPort = Integer.parseInt(args[2]);
        Client client = new Client(port);
        client.start();
        // First connection
        try(Socket socket = new Socket(peerIp, peerPort)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // TODO create factory class that automatically creates message, also possibly implement sort of command pattern to avoid switch peer side
            out.println("ADDCLIENT:" + id + ":" + "127.0.0.1:" + port);
        } catch (IOException e) {
            System.out.println("Error during connection to " + peerIp + ":" + peerPort);
        }

        while (true) {
            // System.out.print("Enter [peerIp peerPort queueId value] or 'quit': ");
            System.out.print("Enter [queueID value] or 'quit': ");
            String line = scanner.nextLine();
            if (line.trim().equalsIgnoreCase("quit")) {
                scanner.close();
                break;
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length != 2) {
                System.out.println("Invalid input. Please enter exactly 2 values.");
                continue;
            }

//            String peerIp = parts[0];
//            int peerPort = Integer.parseInt(parts[1]);
            try (Socket socket = new Socket(peerIp, peerPort)) {
                String queueId = parts[0];
                String value = parts[1];
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("ADD:" + id + ":" + queueId + ":" + value);
                synchronized (lock) {
                    System.out.println("Sent to " + peerIp + ":" + peerPort);
                    lock.notify();
                    lock.wait();
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Failed to send to " + peerIp + ":" + peerPort);
            }
        }

    }
}
