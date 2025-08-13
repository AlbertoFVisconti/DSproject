package client;

import common.HandlerRegistry;
import common.messageHandlers.AckHandler;
import common.messageHandlers.NAckHandler;
import common.MessageType;
import common.messageHandlers.NewLeaderHandler;
import common.messageHandlers.ValueResponseHandler;
import common.messages.*;
import common.util.NewLeaderException;

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
    private String peerIp;
    private int peerPort;

    public Client(String ip, int port) {
        this.ip = ip;
        this.port = port;
        registry.registerHandler(MessageType.ACK, new AckHandler(lock));
        registry.registerHandler(MessageType.NACK, new NAckHandler(lock));
        registry.registerHandler(MessageType.VALRES, new ValueResponseHandler(lock));
        registry.registerHandler(MessageType.NEWLEADER, new NewLeaderHandler(peerIp, peerPort));
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
                Message msg = registry.deserialize(message);
                try {
                    registry.handle(msg);
                } catch (NewLeaderException e) {
                    System.out.println("\n");
                    System.out.println(e.getMessage());
                    // This is not pretty but should be safe
                    NewLeaderMessage nlm = (NewLeaderMessage) msg;
                    this.peerIp = nlm.getLeaderIp();
                    this.peerPort = nlm.getLeaderPort();
                    System.out.println("Press enter to continue...");
                }
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
        AppendValueMessage appendVal = new AppendValueMessage(UUID.randomUUID(), queueId, value, null);
        appendVal.setSenderId(id.toString());
        out.println(appendVal.serialize());
    }

    public void createQueue(PrintWriter out, String queueId) {
        CreateQueueMessage createQueue = new CreateQueueMessage(UUID.randomUUID(), queueId,null);
        createQueue.setSenderId(id.toString());
        out.println(createQueue.serialize());
    }

    public void readFromQueue(PrintWriter out, String queueId) {
        ReadValueMessage readQueue = new ReadValueMessage(UUID.randomUUID(), queueId, null);
        readQueue.setSenderId(id.toString());
        out.println(readQueue.serialize());
    }

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        Scanner scanner = new Scanner(System.in);
        if (args.length != 4) {
            System.out.println("Usage: java Client <clientIp> <clientPort> <peerIp> <peerPort>");
            scanner.close();
            return;
        }
        int counter = 0;
        String  ip = args[0];
        int port = Integer.parseInt(args[1]);
        Client client = new Client(ip, port);
        client.peerIp = args[2];
        client.peerPort = Integer.parseInt(args[3]);
        client.start();
        // First connection, try three times and if failed connect to leader
        while (counter < 3) {
            try (Socket socket = new Socket(client.peerIp, client.peerPort)) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                client.sendAddClient(out);
                // Wait till we receive the ACK from the leader
                synchronized (lock) {
                    try {
                        long start = System.currentTimeMillis();
                        lock.wait(2000); // Wait for up to 2 seconds
                        long elapsed = System.currentTimeMillis() - start;
                        if (elapsed >= 2000) {
                            System.out.println("Did not receive any ack by 2 seconds!");
                            System.out.println("Exiting ...");
                            System.exit(1);
                        }
                    } catch (InterruptedException e) {
                        System.out.println("No ACK received from peer! Exiting ...");
                        System.exit(1);
                    }
                }
                break;
            } catch (IOException e) {
                System.out.println("Error during connection to " + client.peerIp + ":" + client.peerPort + ", please try again!");
                counter++;
            }
            System.out.print("Enter peer ip: ");
            client.peerIp = scanner.nextLine();
            System.out.print("Enter peer port: ");
            client.peerPort = Integer.parseInt(scanner.nextLine());
        }
        if (counter == 3) {
            System.out.println("Connection to peer failed three times in a row, please ensure that it is online! Exiting ...");
            System.exit(1);
        }
        while (true) {
            System.out.print("Enter 'create [queueId]' or 'add [queueID value]' or 'read [queueID]' or 'quit': ");
            String line = scanner.nextLine();
            if (line.trim().equalsIgnoreCase("quit")) {
                scanner.close();
                break;
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 2 || parts.length > 3) {
                System.out.println("Invalid input. Please enter either 'create [queueId]' or 'add [queueId value]'.");
                continue;
            }
            String command = parts[0];

            try (Socket socket = new Socket(client.peerIp, client.peerPort)) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                if (command.equalsIgnoreCase("create") && parts.length == 2) {
                    String queueId = parts[1];
                    client.createQueue(out, queueId);
                } else if (command.equalsIgnoreCase("add") && parts.length == 3) {
                    String queueId = parts[1];
                    int value = Integer.parseInt(parts[2]);
                    client.appendValue(out, queueId, value);
                } else if (command.equalsIgnoreCase("read") && parts.length == 2) {
                    String queueId = parts[1];
                    client.readFromQueue(out, queueId);
                } else {
                    System.out.println("Invalid command format.");
                    continue;
                }

                synchronized (lock) {
                    System.out.println("Sent to " + client.peerIp + ":" + client.peerPort);
                    long start = System.currentTimeMillis();
                    lock.wait(2000); // Wait for up to 2 seconds
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed >= 2000) {
                        System.out.println("Didn't receive any confirmation, please try again!");
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Failed to send to " + client.peerIp + ":" + client.peerPort);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format for value.");
            }
        }
        System.out.println("Bye!");
        System.exit(0);
    }
}
