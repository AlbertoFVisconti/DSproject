package peer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import common.MessageParser;
import common.MessageType;
import raft.Role;
import tpc.State;

public class Peer {
    private final int port;
    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final Map<String, String> peerAddresses = new ConcurrentHashMap<>(); // id -> "ip:port"
    private final Map<String, String> clientAddresses = new ConcurrentHashMap<>(); // id -> "ip:port"
    private Role role;
    private State state;
    private String leader;

    public Peer(int port) {
        this.port = port;
        this.role = Role.LEADER;
        this.state = State.INIT;
    }

    public void start() {
        new Thread(this::listenForMessages).start();
        new Thread(this::DebugInfo).start();
        System.out.println("[" + id + "] Listening on port " + port);
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

    // recived a message to Socket from a peer
    private void handleIncomingMessage(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(":");
                MessageType type = MessageParser.parseType(message);

                if (type == null) {
                    System.out.println("[" + id + "] Unknown message type: " + parts[0]);
                    continue;
                }

                switch (type) {
                    case MessageType.PEER:
                        String newId = parts[1];
                        String ip = parts[2];
                        String port = parts[3];
                        String address = ip + ":" + port;

                        if (!peerAddresses.containsKey(newId)) {
                            peerAddresses.put(newId, address);
                            System.out.println("[" + id + "] Discovered new peer: " + newId + " at " + address);
                            broadcast("PEER:" + newId + ":" + ip + ":" + port, newId);
                            connectToPeer(ip, Integer.parseInt(port));
                        }
                        break;

                    case MessageType.ADD:
                        if (role == Role.LEADER) {
                            // start 3PC procedure to add the value
                            // TODO implement
                        } else {
                            // send it to the leader if possible, otherwise drop it
                            if (leader == null) {
                                return;
                            }

                            contact(leader, message);

                        }
                        break;
                    case MessageType.ADDCLIENT:
                        String client_id = parts[1];
                        String client_ip = parts[2];
                        String client_port = parts[3];
                        String client_address = client_ip + ":" + client_port;
                        if (!clientAddresses.containsKey(client_id)) {
                            clientAddresses.put(client_id, client_address);
                            System.out.println(
                                    "[" + id + "] Discovered new client: " + client_id + " at " + client_address);
                            broadcast("ADDCLIENT:" + client_id + ":" + client_ip + ":" + client_port, null);
                        }

                    default:
                        System.out.println("[" + id + "] Unknown message type: " + type);
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("[" + id + "] Peer connection error");
        }
    }

    public void connectToPeer(String host, int peerPort) {
        try {
            Socket socket = new Socket(host, peerPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("PEER:" + id + ":localhost:" + port);
            System.out.println("[" + id + "] Connected to " + host + ":" + peerPort);
            socket.close();
        } catch (IOException e) {
            System.out.println("[" + id + "] Failed to connect to " + host + ":" + peerPort);
        }
    }

    private void contact(String peer_id, String message) {
        if (!peerAddresses.containsKey(peer_id)) {
            return;
        }
        String peer_info = peerAddresses.get(peer_id);
        String values[] = peer_info.split(":");
        String peerIP = values[0];
        String peerPORT = values[1];
        try {
            Socket socket = new Socket(peerIP, Integer.parseInt(peerPORT));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
            socket.close();
        } catch (IOException e) {
            System.out.println("[" + id + "] Failed to send to " + peer_id);
        }

    }

    private void broadcast(String message, String excludeId) {
        for (Map.Entry<String, String> entry : peerAddresses.entrySet()) {
            String peerId = entry.getKey();
            String values[] = entry.getValue().split(":");
            String peerIP = values[0];
            String peerPORT = values[1];
            if (!peerId.equals(excludeId)) {
                try {
                    Socket socket = new Socket(peerIP, Integer.parseInt(peerPORT));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(message);
                    socket.close();
                } catch (IOException e) {
                    System.out.println("[" + id + "] Failed to send to " + peerId);
                }
            }
        }
    }

    private void DebugInfo() {
        while (true) {
            System.out.println("[" + this.role + "]");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        if (args.length < 1) {
            System.out.println("Usage: java Peer <port> [peerHost peerPort]");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Peer peer = new Peer(port);
        if (args.length == 3) {
            peer.role = Role.FOLLOWER;
        }
        peer.start();

        if (args.length == 3) {
            String peerHost = args[1];
            int peerPort = Integer.parseInt(args[2]);
            peer.connectToPeer(peerHost, peerPort);
        }
    }
}
