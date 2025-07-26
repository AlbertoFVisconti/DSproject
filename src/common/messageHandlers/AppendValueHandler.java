package common.messageHandlers;

import common.messages.*;
import common.util.NotLeaderException;
import peer.Peer;
import peer.QueueStore;
import raft.Role;

import java.util.Optional;
import java.util.UUID;

public class AppendValueHandler extends Handler<AppendValueMessage> {
    private final QueueStore queue;
    private final Peer peer;
    public AppendValueHandler(QueueStore queue, Peer peer) {
        this.queue = queue;
        this.peer = peer;
    }

    @Override
    public Optional<Response> visit(AppendValueMessage message) throws NotLeaderException {
        String queueId = message.getQueueId();
        String clientId = message.getSenderId();
        int value = message.getValue();
        System.out.println("Recived message: "+message.serialize());
        if (peer.getRole() == Role.LEADER && message.getLeaderId()==null) {
            AppendValueMessage appendValueMessage= new AppendValueMessage(message.getUuid(), queueId, value, peer.getId().toString());
            appendValueMessage.setSenderId(clientId);
            peer.broadcast(appendValueMessage.serialize(), peer.getId().toString());
            try {
                queue.addValue(queueId, clientId, value);
                return Optional.of(new AckMessage(message.getUuid()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Queue with id " + queueId + " does not exist");
            }
        } else if (peer.getRole()==Role.FOLLOWER && message.getLeaderId()!=null) {
            try {
                queue.addValue(queueId, clientId, value);
                return Optional.of(new AckMessage(message.getUuid()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Queue with id " + queueId + " does not exist");
            }
        } else if (peer.getRole()==Role.FOLLOWER && message.getLeaderId()==null) {
            if(peer.getLeader()==null) {System.out.print("No leader found, letting it timeout");}
            else{
                System.out.println("Forwarding to leader: "+this.peer.getLeader());
                peer.contactPeer(peer.getLeader(), message);}
        }
        return Optional.empty();
    }

    @Override
    public AppendValueMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        String queueId = parts[2];
        int value = Integer.parseInt(parts[3]);
        String leaderId =null;
        if(parts.length == 5) {
            leaderId = parts[4];
        }
        AppendValueMessage msg = new AppendValueMessage(id, queueId, value, leaderId);
        msg.setSenderId(senderId);
        return msg;
    }

}
