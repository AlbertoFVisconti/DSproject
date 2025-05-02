package common.messageHandlers;

import common.messages.PeerMessage;
import common.messages.Response;
import common.util.NewPeerFoundException;
import peer.AddressRegistry;

import java.util.Optional;
import java.util.UUID;

public class PeerHandler extends Handler<PeerMessage> {
    AddressRegistry peerRegistry;

    public PeerHandler(AddressRegistry registry) {
        this.peerRegistry = registry;
    }

    @Override
    public Optional<Response> visit(PeerMessage msg) {
        String id = msg.getSenderId();
        String ip = msg.getIp();
        int port = msg.getPort();
        if(peerRegistry.getAddress(id) == null) {
            peerRegistry.addEntry(id, ip + ":" + port);
            throw new NewPeerFoundException("New peer " + id + " found. Forwarding to all peers.");
        }
        peerRegistry.addEntry(id, ip + ":" + port);
        return Optional.empty();
    }
    @Override
    public PeerMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        String ip = parts[2];
        int port = Integer.parseInt(parts[3]);
        PeerMessage msg = new PeerMessage(id, ip, port);
        msg.setSenderId(senderId);
        return msg;
    }
}
