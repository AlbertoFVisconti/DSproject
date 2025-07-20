package peer;

import java.io.*;
import java.net.*;
import java.util.*;

import common.HandlerRegistry;
import common.MessageType;
import common.messageHandlers.AddClientHandler;
import common.messageHandlers.AppendValueHandler;
import common.messageHandlers.CreateQueueHandler;
import common.messageHandlers.PeerHandler;
import common.messages.Message;
import common.messages.NAckMessage;
import common.messages.PeerMessage;
import common.messages.Response;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;
import raft.Role;
import tpc.State;

public class Peer {
    private final int port;
    private final UUID id = UUID.randomUUID();
    private final AddressRegistry peerAddresses = new AddressRegistry(); // id -> "ip:port"
    private final AddressRegistry clientAddresses = new AddressRegistry(); // id -> "ip:port"
    // TODO possible singleton for these two
    private final HandlerRegistry registry = new HandlerRegistry();
    private final QueueStore queueStore = new QueueStore();

    private Role role;
    // TODO this needs to be set
    private State state;
    // TODO set this with PEER response
    private String leader;

    private List<String> DebugInfo;

    public Peer(int port) {
        this.port = port;
        this.role = Role.LEADER;
        this.state = State.INIT;
        registry.registerHandler(MessageType.ADDCLIENT, new AddClientHandler(clientAddresses, role));
        registry.registerHandler(MessageType.APPENDVALUE, new AppendValueHandler(queueStore, role));
        registry.registerHandler(MessageType.PEER, new PeerHandler(peerAddresses));
        registry.registerHandler(MessageType.CREATEQUEUE, new CreateQueueHandler(queueStore, role));

        this.DebugInfo = new ArrayList<>();
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
            System.err.println(e.getMessage());
        }
    }

    // recived a message to Socket from a peer
    private void handleIncomingMessage(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            while ((message = in.readLine()) != null) {
                Message msg = registry.deserialize(message);
                try {
                    Optional<Response> ret = registry.handle(msg);
                    if(ret.isPresent()) {
                        Response res = ret.get();
                        res.setSenderId(id.toString());
                        contactClient(msg.getSenderId(), res);
                    }
                } catch (NotLeaderException e) {
                    System.out.println(e.getMessage());
                    //TODO send message to leader
                } catch (NewPeerFoundException | NewClientFoundException e) {
                    System.out.println(e.getMessage());
                    broadcast(msg.serialize(), id.toString());
                } catch (NoSuchFieldError e) {
                    Response res = new NAckMessage(msg.getUuid());
                    res.setSenderId(id.toString());
                    contactClient(msg.getSenderId(), res);
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
            PeerMessage pm = new PeerMessage(UUID.randomUUID(), host, peerPort);
            pm.setSenderId(id.toString());
            out.println(pm.serialize());
            System.out.println("[" + id + "] Connected to " + host + ":" + peerPort);
            socket.close();
        } catch (IOException e) {
            System.out.println("[" + id + "] Failed to connect to " + host + ":" + peerPort);
        }
    }

    public void contactClient(String id, Message message) {
        String client_ip = clientAddresses.getAddress(id).split(":")[0];
        int client_port = Integer.parseInt(clientAddresses.getAddress(id).split(":")[1]);
        try {
            Socket socket = new Socket(client_ip, client_port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message.serialize());
            socket.close();
        } catch (IOException e) {
            System.out.println("[" + id + "] Failed to connect to " + client_ip + ":" + client_port);
        }
    }

    private void broadcast(String message, String excludeId) {
        for (String entry : peerAddresses.getIds()) {
            String[] values = peerAddresses.getAddress(entry).split(":");
            String peerIP = values[0];
            String peerPORT = values[1];
            if (!entry.equals(excludeId)) {
                try {
                    Socket socket = new Socket(peerIP, Integer.parseInt(peerPORT));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(message);
                    socket.close();
                } catch (IOException e) {
                    System.out.println("[" + id + "] Failed to send to " + entry);
                }
            }
        }
    }
    public void  addDebugInfo(String info) {
        DebugInfo.add(info);
    }

    private void DebugInfo() {
        while (true) {
            for(int i=0;i<DebugInfo.size();i++) {
                System.out.println(DebugInfo.get(i));
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
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
        peer.queueStore.addQueue("2");
        if (args.length == 3) {
            String peerHost = args[1];
            int peerPort = Integer.parseInt(args[2]);
            peer.connectToPeer(peerHost, peerPort);
        }
    }
}
