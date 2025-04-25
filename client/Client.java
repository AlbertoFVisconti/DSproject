package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Client {
    private final int port;
    private final String id = UUID.randomUUID().toString().substring(0, 8);

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
        // TODO ADD LOGIC FOR INCOMING MESSAGES FOR THE CLIENT
        // succesful adding value, failed adding value,
        // ??maybe even saving all of the peers ip so if the one that you know fails you
        // can contact another

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
        Client client = new Client(port);

        while (true) {
            System.out.print("Enter [peerIp peerPort queueId value] or 'quit': ");
            String line = scanner.nextLine();
            if (line.trim().equalsIgnoreCase("quit")) {
                scanner.close();
                break;
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length != 4) {
                System.out.println("Invalid input. Please enter exactly 4 values.");
                continue;
            }

            String peerIp = parts[0];
            int peerPort = Integer.parseInt(parts[1]);
            String queueId = parts[2];
            String value = parts[3];
            try (Socket socket = new Socket(peerIp, peerPort);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println("ADD:" + queueId + ":" + value);
                System.out.println("Sent to " + peerIp + ":" + peerPort);
            } catch (IOException e) {
                System.out.println("Failed to send to " + peerIp + ":" + peerPort);
            }
        }

    }
}
