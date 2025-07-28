package common.messageHandlers;

import common.messages.CandidateMessage;
import common.messages.PingMessage;
import common.messages.Response;
import peer.LeaderHandler;
import peer.Peer;
import raft.Role;

import java.util.Optional;
import java.util.UUID;

public class PingHandler extends Handler<PingMessage> {
    private LeaderHandler leaderHandler;
    public PingHandler(LeaderHandler leaderHandler) {
        super();
        this.leaderHandler = leaderHandler;
    }
    @Override
    public Optional<Response> visit(PingMessage message) {
        if(this.leaderHandler.isListening()){
            Peer peer =this.leaderHandler.getPeer();
            if (peer.getRole()== Role.FOLLOWER)
                leaderHandler.setLeader(message.getLeader_uuid());
            else if (peer.getRole()== Role.LEADER) {
                CandidateMessage msg = new CandidateMessage(UUID.randomUUID(), peer.getValue());
                msg.setSenderId(peer.getId().toString());
                peer.broadcast(msg.serialize(), "");
            }
        }
        this.leaderHandler.receivedPing();
        return Optional.empty();
    }
    @Override
    public PingMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String leaderId = parts[1];
        PingMessage msg=new PingMessage(id, leaderId);
        msg.setSenderId(leaderId);
        return msg;
    }
}
