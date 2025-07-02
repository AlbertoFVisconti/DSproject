package common.messageHandlers;

import common.messages.*;
import common.util.NotLeaderException;
import peer.QueueStore;
import raft.Role;

import java.util.Optional;
import java.util.UUID;

public class AppendValueHandler extends Handler<AppendValueMessage> {
    private final QueueStore queue;
    private Role recvRole;
    public AppendValueHandler(QueueStore queue, Role role) {
        this.queue = queue;
        this.recvRole = role;
    }

    @Override
    public Optional<Response> visit(AppendValueMessage message) throws NotLeaderException {
        String queueId = message.getQueueId();
        String clientId = message.getSenderId();
        int value = message.getValue();
        if (recvRole != Role.LEADER) {
            throw new NotLeaderException("Peer is not the leader.");
        }
        try {
            //TODO if there is no leader do not send it (let it timeout)
            //if there is leader send it to him
            queue.addValue(queueId, clientId, value);
            return Optional.of(new AckMessage(message.getUuid()));
        } catch (IllegalArgumentException e) {
            throw new NoSuchFieldError("Queue with id " + queueId + " does not exist");
        }
    }

    @Override
    public AppendValueMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        String queueId = parts[2];
        int value = Integer.parseInt(parts[3]);
        AppendValueMessage msg = new AppendValueMessage(id, queueId, value);
        msg.setSenderId(senderId);
        return msg;
    }

    public void setRecvRole(Role role) {
        this.recvRole = role;
    }

    public Role getRecvRole() {
        return recvRole;
    }
}
