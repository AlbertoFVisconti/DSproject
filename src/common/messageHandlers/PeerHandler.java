package common.messageHandlers;

import common.messages.PeerMessage;
import common.messages.Response;
import common.messages.UpdateMessage;
import common.util.NewPeerFoundException;
import peer.AddressRegistry;
import peer.Peer;

import java.util.Optional;
import java.util.UUID;

public class PeerHandler extends Handler<PeerMessage> {
    AddressRegistry peerRegistry;
    Peer peer;
    public PeerHandler(AddressRegistry registry, Peer peer) {
        this.peerRegistry = registry;
        this.peer = peer;
    }

    @Override
    public Optional<Response> visit(PeerMessage msg) {
        String id = msg.getSenderId();
        String ip = msg.getIp();
        int port = msg.getPort();
        if(peer.getValue()>0){
            UpdateMessage updateMessage=new UpdateMessage(this.peer.getId(), peer.getValue(), peer.getQueueStore(), peer.getQueueStore().getClientQueues());
            updateMessage.setSenderId(this.peer.getId().toString());
            peer.contactPeer(msg.getSenderId(),updateMessage);
        }
        if(peerRegistry.getAddress(id) == null) {
            peerRegistry.addEntry(id, ip + ":" + port);
            PeerMessage peerMessage=new PeerMessage(this.peer.getId(), peer.getIp(),peer.getPort());
            peerMessage.setSenderId(this.peer.getId().toString());
            peer.contactPeer(msg.getSenderId(),peerMessage);
            throw new NewPeerFoundException("New peer " + id.substring(0, 8) + "@" + ip + " found. Forwarding to all peers.");

        }
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
