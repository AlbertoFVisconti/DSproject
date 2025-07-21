package common.messageHandlers;

import common.messages.PingMessage;
import common.messages.Response;
import peer.LeaderHandler;

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
        return Optional.empty();
    }
    @Override
    public PingMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String leaderId = parts[1];
        PingMessage msg=new PingMessage(id, leaderId);
        msg.setSenderId(leaderId);
        this.leaderHandler.receivedPing();
        return msg;
    }
}
