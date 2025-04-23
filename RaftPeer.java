import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RaftPeer {
    private final int port;
    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final Map<String, String> peerAddresses = new ConcurrentHashMap<>(); // id -> "ip:port"

    public RaftPeer(int port) {
        this.port = port;
    }

    public void start() {
        new Thread(this::listenForPeers).start();
        new Thread(this::PrintPeers).start();
        System.out.println("[" + id + "] Listening on port " + port);
    }

    private void listenForPeers() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleIncomingPeer(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // recived a message to Socket from a peer
    private void handleIncomingPeer(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String line;
            while ((line = in.readLine()) != null) {
                // different message type
                if (line.startsWith("PEER:")) {
                    String[] parts = line.split(":");
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

    private void PrintPeers() {
        while (true) {
            System.out.println("Current known peers are:");
            for (Map.Entry<String, String> entry : peerAddresses.entrySet()) {
                System.out.println(entry.getKey() + "AT:  " + entry.getValue());
            }
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                System.err.println("Error in the print timeout");
                return;
            }
        }
    }

    public static void main(String[] args) {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        if (args.length < 1) {
            System.out.println("Usage: java RaftPeer <port> [peerHost peerPort]");
            return;
        }

        int port = Integer.parseInt(args[0]);
        RaftPeer peer = new RaftPeer(port);
        peer.start();

        if (args.length == 3) {
            String peerHost = args[1];
            int peerPort = Integer.parseInt(args[2]);
            peer.connectToPeer(peerHost, peerPort);
        }
    }
}
