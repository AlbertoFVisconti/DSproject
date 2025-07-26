package client;

import common.HandlerRegistry;
import common.messageHandlers.AckHandler;
import common.messageHandlers.NAckHandler;
import common.MessageType;
import common.messageHandlers.ValueResponseHandler;
import common.messages.AddClientMessage;
import common.messages.AppendValueMessage;
import common.messages.CreateQueueMessage;
import common.messages.ReadValueMessage;

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
        registry.registerHandler(MessageType.VALRES, new ValueResponseHandler(lock));
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
        ReadValueMessage readQueue = new ReadValueMessage(UUID.randomUUID(), queueId);
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
        String  ip = args[0];
        int port = Integer.parseInt(args[1]);
        String peerIp = args[2];
        int peerPort = Integer.parseInt(args[3]);
        Client client = new Client(ip, port);
        client.start();
        // First connection
        try(Socket socket = new Socket(peerIp, peerPort)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            client.sendAddClient(out);
        } catch (IOException e) {
            System.out.println("Error during connection to " + peerIp + ":" + peerPort);
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

            try (Socket socket = new Socket(peerIp, peerPort)) {
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
                    System.out.println("Sent to " + peerIp + ":" + peerPort);
                    long start = System.currentTimeMillis();
                    lock.wait(2000); // Wait for up to 2 seconds
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed >= 2000) {
                        System.out.println("unable to send");
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Failed to send to " + peerIp + ":" + peerPort);
            }catch (NumberFormatException e) {
                System.out.println("Invalid number format for value.");
            }
        }
        System.out.println("Bye!");
        System.exit(0);
    }
}
