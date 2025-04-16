import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RaftPeer {
    private final int port;
    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final Map<String, Socket> peerSockets = new ConcurrentHashMap<>();
    private final Map<String, String> peerAddresses = new ConcurrentHashMap<>(); // id -> "ip:port"

    public RaftPeer(int port) {
        this.port = port;
    }

    public void start() {
        new Thread(this::listenForPeers).start();
        new Thread(this::sendPings).start();
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

    private void handleIncomingPeer(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("PEER:")) {
                    String[] parts = line.split(":");
                    String newId = parts[1];
                    String ip = parts[2];
                    String port = parts[3];
                    String address = ip + ":" + port;

                    if (!peerSockets.containsKey(newId)) {
                        try {
                            Socket s = new Socket(ip, Integer.parseInt(port));
                            peerSockets.put(newId, s);
                            peerAddresses.put(newId, address);
                            System.out.println("[" + id + "] Discovered new peer: " + newId + " at " + address);
                            broadcast("PEER:" + newId + ":" + ip + ":" + port, newId);
                        } catch (IOException e) {
                            System.out.println("[" + id + "] Could not connect to " + address);
                        }
                    }
                } else if (line.startsWith("PING:")) {
                    System.out.println("[" + id + "] Received " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("[" + id + "] Peer connection error");
        }
    }

    public void connectToPeer(String host, int peerPort) {
        try {
            Socket socket = new Socket(host, peerPort);
            peerSockets.put("TEMP", socket); // placeholder until we get their ID
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("PEER:" + id + ":localhost:" + port);
            System.out.println("[" + id + "] Connected to " + host + ":" + peerPort);
        } catch (IOException e) {
            System.out.println("[" + id + "] Failed to connect to " + host + ":" + peerPort);
        }
    }

    private void broadcast(String message, String excludeId) {
        for (Map.Entry<String, Socket> entry : peerSockets.entrySet()) {
            String peerId = entry.getKey();
            if (!peerId.equals(excludeId)) {
                try {
                    PrintWriter out = new PrintWriter(entry.getValue().getOutputStream(), true);
                    out.println(message);
                } catch (IOException e) {
                    System.out.println("[" + id + "] Failed to send to " + peerId);
                }
            }
        }
    }

    private void sendPings() {
        while (true) {
            try {
                List<String> peerIds = new ArrayList<>(peerSockets.keySet());
                Collections.shuffle(peerIds);
                for (int i = 0; i < Math.min(2, peerIds.size()); i++) {
                    String targetId = peerIds.get(i);
                    Socket socket = peerSockets.get(targetId);
                    if (socket != null && !socket.isClosed()) {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("PING:" + id);
                    }
                }
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
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
