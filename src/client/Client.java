package client;

import common.HandlerRegistry;
import common.messageHandlers.AckHandler;
import common.messageHandlers.NAckHandler;
import common.MessageType;
import common.messages.AddClientMessage;
import common.messages.AppendValueMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.UUID;

public class Client {
    private static final Object lock = new Object();
    private final int port;
    private final String ip;
    private final static UUID id = UUID.randomUUID();
    private final HandlerRegistry registry = new HandlerRegistry();

    public Client(String ip, int port) {
        this.ip = ip;
        this.port = port;
        registry.registerHandler(MessageType.ACK, new AckHandler(lock));
        registry.registerHandler(MessageType.NACK, new NAckHandler(lock));
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
            System.err.println("Error during socket connection!");
        }
    }

    private void handleIncomingMessage(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            while ((message = in.readLine()) != null) {
                registry.handle(registry.deserialize(message));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void sendAddClient(PrintWriter out) {
        AddClientMessage addClient = new AddClientMessage(UUID.randomUUID(),this.ip, this.port);
        addClient.setSenderId(id.toString());
        out.println(addClient.serialize());
    }

    public void appendValue(PrintWriter out, String queueId, int value) {
        AppendValueMessage appendVal = new AppendValueMessage(UUID.randomUUID(), queueId, value);
        appendVal.setSenderId(id.toString());
        out.println(appendVal.serialize());
    }

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
        Client client = new Client("localhost", port);
        client.start();
        // First connection
        try(Socket socket = new Socket(peerIp, peerPort)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            client.sendAddClient(out);
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
            try (Socket socket = new Socket(peerIp, peerPort)) {
                String queueId = parts[0];
                int value = Integer.parseInt(parts[1]);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                client.appendValue(out, queueId, value);
                synchronized (lock) {
                    System.out.println("Sent to " + peerIp + ":" + peerPort);
                    lock.wait();
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Failed to send to " + peerIp + ":" + peerPort);
            }
        }

    }
}
