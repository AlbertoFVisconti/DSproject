package common.messageHandlers;

import common.messages.AckMessage;
import common.messages.AppendValueMessage;
import common.messages.CreateQueueMessage;
import common.messages.Response;
import common.util.NotLeaderException;
import peer.Peer;
import peer.QueueStore;
import raft.Role;

import java.util.Optional;
import java.util.UUID;

public class CreateQueueHandler extends Handler<CreateQueueMessage> {
    private final QueueStore queue;
    private final Peer peer;
    public CreateQueueHandler(QueueStore queue, Peer peer) {
        this.queue = queue;
        this.peer = peer;
    }

    public Optional<Response> visit(CreateQueueMessage message) throws NotLeaderException {
        String queueId = message.getQueueId();
        System.out.println("Recived message: "+message.serialize());
        if (peer.getRole() == Role.LEADER && message.getLeaderId()==null) {
            CreateQueueMessage createQueueMessage = new CreateQueueMessage(message.getUuid(),queueId,this.peer.getId().toString());
            createQueueMessage.setSenderId(message.getSenderId());
            peer.broadcast(createQueueMessage.serialize(), peer.getId().toString());
            try {
                queue.addQueue(queueId);
                return Optional.of(new AckMessage(message.getUuid()));
            } catch (IllegalArgumentException e) {
                throw new NoSuchFieldError("Queue with id " + queueId + " does not exist");
            }
        } else if (peer.getRole()==Role.FOLLOWER && message.getLeaderId()!=null) {
            try {
                queue.addQueue(queueId);
                return Optional.of(new AckMessage(message.getUuid()));
            } catch (IllegalArgumentException e) {
                throw new NoSuchFieldError("Queue with id " + queueId + " does not exist");
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
    public CreateQueueMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        String queueId = parts[2];
        String leaderId =null;
        if(parts.length == 4) {
            leaderId = parts[3];
        }
        CreateQueueMessage msg = new CreateQueueMessage(id, queueId,leaderId);
        msg.setSenderId(senderId);
        return msg;
    }
}
