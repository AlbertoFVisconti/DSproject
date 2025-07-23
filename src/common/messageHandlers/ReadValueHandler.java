package common.messageHandlers;

import com.sun.jdi.Value;
import common.messages.*;
import common.util.NotLeaderException;
import peer.QueueStore;
import raft.Role;

import java.util.Optional;
import java.util.UUID;

public class ReadValueHandler extends Handler<ReadValueMessage> {
    private final QueueStore queue;
    private Role recvRole;

    public ReadValueHandler(QueueStore queue, Role role) {
        this.queue = queue;
        this.recvRole = role;
    }

    @Override
    public Optional<Response> visit(ReadValueMessage message) throws NotLeaderException {
        String queueId = message.getQueueId();
        String clientId = message.getSenderId();
        if (recvRole != Role.LEADER) {
            throw new NotLeaderException("Peer is not the leader.");
        }
        try {
            //TODO if there is no leader do not send it (let it timeout)
            //if there is leader send it to him
            int val = queue.readValue(queueId, clientId);
            ValueResponse res = new ValueResponse(message.getUuid());
            res.setValue(val);
            return Optional.of(res);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    @Override
    public ReadValueMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        String queueId = parts[2];
        ReadValueMessage msg = new ReadValueMessage(id, queueId);
        msg.setSenderId(senderId);
        return msg;
    }
}
