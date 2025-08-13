package peer;

import java.io.*;
import java.net.*;
import java.util.*;

import common.HandlerRegistry;
import common.MessageType;
import common.messageHandlers.*;
import common.messages.*;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;
import raft.Role;

import javax.print.attribute.standard.MediaSize;

public class Peer {
    private final String ip;
    private final int port;
    private final UUID id = UUID.randomUUID();
    private final AddressRegistry peerAddresses = new AddressRegistry(); // id -> "ip:port"
    private final AddressRegistry clientAddresses = new AddressRegistry(); // id -> "ip:port"
    // TODO possible singleton for these two
    private final HandlerRegistry registry = new HandlerRegistry();
    private final QueueStore queueStore = new QueueStore();
    private final Object writingLock = new Object();
    private final Object roleLock = new Object();

    private Role role;
    private String leader;
    private final LeaderHandler leaderHandler;

    // Used only for prints to make the CLI more readable and less verbose
    public String truncateUUID(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }

    public Peer(String ip, int port) {
        this.ip=ip;
        this.port = port;
        this.role = Role.LEADER;
        this.leaderHandler = new LeaderHandler();
        peerAddresses.addEntry(this.id.toString(), ip + ":" + port);
        registry.registerHandler(MessageType.ADDCLIENT, new AddClientHandler(clientAddresses, role));
        registry.registerHandler(MessageType.APPENDVALUE, new AppendValueHandler(queueStore, this));
        registry.registerHandler(MessageType.PEER, new PeerHandler(peerAddresses, this));
        registry.registerHandler(MessageType.CREATEQUEUE, new CreateQueueHandler(queueStore, this));
        registry.registerHandler(MessageType.PING, new PingHandler(leaderHandler));
        registry.registerHandler(MessageType.CANDIDATE, new CandidateHandler(this));
        registry.registerHandler(MessageType.READVALUE, new ReadValueHandler(queueStore, this));
        registry.registerHandler(MessageType.UPDATE, new UpdateHandler(this.queueStore));
    }

    public void start() {
        new Thread(this::listenForMessages).start();
        leaderHandler.start(this);
        System.out.println("[" + truncateUUID(id) + "] Listening on port " + port);
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
                    // Only leader talks back to client
                    if(ret.isPresent() && this.role == Role.LEADER) {
                        Response res = ret.get();
                        res.setSenderId(id.toString());
                        contactClient(msg.getSenderId(), res);
                    }
                } catch (NotLeaderException e) {
                    System.out.println(e.getMessage());
                } catch (NewPeerFoundException | NewClientFoundException e) {
                    System.out.println(e.getMessage());
                    // If this is the leader, confirm to client they are connected and forward new client message to followers
                    if(this.role == Role.LEADER && e instanceof NewClientFoundException) {
                        broadcast(msg.serialize(), id.toString());
                        Response res = new AckMessage(msg.getUuid());
                        res.setSenderId(id.toString());
                        contactClient(msg.getSenderId(), res);
                    }
                    // Since peers connect to the leader, only him needs to forward the new peer message
                    if(this.role == Role.LEADER && e instanceof NewPeerFoundException) {
                        broadcast(msg.serialize(), id.toString());
                    }
                } catch (IllegalArgumentException e) {
                    if(this.role == Role.LEADER) {
                        NAckMessage res = new NAckMessage(msg.getUuid());
                        res.setError(e.getMessage());
                        res.setSenderId(id.toString());
                        contactClient(msg.getSenderId(), res);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[" + truncateUUID(id) + "] Peer connection error");
        }
    }

    public void connectToPeer(String host, int peerPort) {
        synchronized (writingLock) {
            try {
                Socket socket = new Socket(host, peerPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                PeerMessage pm = new PeerMessage(UUID.randomUUID(), this.ip, this.port);
                pm.setSenderId(id.toString());
                out.println(pm.serialize());
                System.out.println("[" + truncateUUID(id) + "] Connected to " + host + ":" + peerPort);
                socket.close();
            } catch (IOException e) {
                System.out.println("[" + truncateUUID(id) + "] Failed to connect to " + host + ":" + peerPort);
            }
        }
    }

    public void contactClient(String id, Message message) {
        synchronized (writingLock) {
            String client_ip = clientAddresses.getAddress(id).split(":")[0];
            int client_port = Integer.parseInt(clientAddresses.getAddress(id).split(":")[1]);
            try {
                Socket socket = new Socket(client_ip, client_port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(message.serialize());
                socket.close();
            } catch (IOException e) {
                System.out.println("[" + truncateUUID(this.id) + "] Failed to connect to " + client_ip + ":" + client_port);
            }
}
    }

    public void broadcast(String message, String excludeId) {
        synchronized (writingLock) {
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
                        System.out.println("[" + truncateUUID(id) + "] Failed to send to " + entry);
                        this.peerAddresses.removeEntry(entry);
                    }
                }
            }
        }
    }
    public void contactPeer(String id, Message message) {
        synchronized (writingLock) {
            if(peerAddresses.getIds().contains(id)) {
                String peerIP = peerAddresses.getAddress(id).split(":")[0];
                String peerPORT = peerAddresses.getAddress(id).split(":")[1];
                try {
                    Socket socket = new Socket(peerIP, Integer.parseInt(peerPORT));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(message.serialize());
                    socket.close();
                }catch (IOException e) {
                    System.out.println("[" + truncateUUID(this.id) + "] Failed to send to " + id);
                    this.peerAddresses.removeEntry(id);
                }
            }
            else{
                System.out.println("[" + truncateUUID(this.id) + "] Peer " + id + " not found");
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Peer <peerIp> <peerPort> <peerHost> <peerPort>");
            return;
        }

        String peerIp = args[0];
        int port = Integer.parseInt(args[1]);
        Peer peer = new Peer(peerIp, port);
        if (args.length == 4) {
            peer.role = Role.FOLLOWER;
        }
        peer.start();
        if (args.length == 4) {
            String peerHost = args[2];
            int peerPort = Integer.parseInt(args[3]);
            peer.connectToPeer(peerHost, peerPort);
        }
    }





    //getters and setters

    public Role getRole() {
        synchronized (roleLock){return role;}
    }
    public void setRole(Role role){synchronized (roleLock){this.role=role;}}

    public UUID getId() {
        return id;
    }
    public int getValue(){
        return this.queueStore.getValue();
    }


    public void setLeader(String leader) {
        synchronized (roleLock){this.leader=leader;}
    }
    public String getLeader() {
        synchronized (roleLock){return this.leader;}
    }

    public LeaderHandler getLeaderHandler() {
        return leaderHandler;
    }
    public String getIp() {return this.ip;}
    public int getPort() {return this.port;}

    public QueueStore getQueueStore() {
        return queueStore;
    }

    public AddressRegistry getClientAddressRegistry() {
        return clientAddresses;
    }
}
