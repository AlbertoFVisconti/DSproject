package common.messageHandlers;

import com.sun.jdi.Value;
import common.messages.*;
import common.util.NotLeaderException;
import peer.Peer;
import peer.QueueStore;
import raft.Role;

import java.util.Optional;
import java.util.UUID;

public class ReadValueHandler extends Handler<ReadValueMessage> {
    private final QueueStore queue;
    private final Peer peer;

    public ReadValueHandler(QueueStore queue, Peer peer) {
        this.queue = queue;
        this.peer = peer;
    }

    @Override
    public Optional<Response> visit(ReadValueMessage message) throws NotLeaderException {
        String queueId = message.getQueueId();
        String clientId = message.getSenderId();
        if(peer.getRole()==Role.LEADER){
            try {
                ReadValueMessage readValueMessage=new ReadValueMessage(message.getUuid(),queueId,this.peer.getId().toString());
                readValueMessage.setSenderId(clientId);
                this.peer.broadcast(readValueMessage.serialize(),this.peer.getId().toString());
                int val = queue.readValue(queueId, clientId);
                ValueResponse res = new ValueResponse(message.getUuid());
                res.setValue(val);
                return Optional.of(res);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }else if(peer.getRole()==Role.FOLLOWER && message.getLeaderId()==null){
            if(peer.getLeader()==null) {System.out.print("No leader found, letting it timeout");}
            else{
                System.out.println("Forwarding to leader: "+this.peer.getLeader());
                peer.contactPeer(peer.getLeader(), message);}
        } else if (peer.getRole()==Role.FOLLOWER && message.getLeaderId()!=null) {
            int val = queue.readValue(queueId, clientId);
            ValueResponse res = new ValueResponse(message.getUuid());
            res.setValue(val);
            return Optional.of(res);
        }
        return Optional.empty();
    }
    @Override
    public ReadValueMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        String queueId = parts[2];
        String leaderId=null;
        if(parts.length==4){
            leaderId = parts[3];
        }
        ReadValueMessage msg = new ReadValueMessage(id, queueId, leaderId);
        msg.setSenderId(senderId);
        return msg;
    }
}
