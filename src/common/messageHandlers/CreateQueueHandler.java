package common.messageHandlers;

import common.messages.AckMessage;
import common.messages.AppendValueMessage;
import common.messages.CreateQueueMessage;
import common.messages.Response;
import common.util.NotLeaderException;
import peer.QueueStore;
import raft.Role;

import java.util.Optional;
import java.util.UUID;

public class CreateQueueHandler extends Handler<CreateQueueMessage> {
    private final QueueStore queue;
    private Role recvRole;
    public CreateQueueHandler(QueueStore queue, Role role) {
        this.queue = queue;
        this.recvRole = role;
    }

    public Optional<Response> visit(CreateQueueMessage message) throws NotLeaderException {
        String queueId = message.getQueueId();
        if (recvRole != Role.LEADER) {
            throw new NotLeaderException("Peer is not the leader.");
        }
        try {
            //TODO if there is no leader do not send it (let it timeout)
            //if there is leader send it to him
            queue.addQueue(queueId);
            return Optional.of(new AckMessage(message.getUuid()));
        } catch (IllegalArgumentException e) {
            throw new NoSuchFieldError("Queue with id " + queueId + " does not exist");
        }
    }

    @Override
    public CreateQueueMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        String queueId = parts[2];
        CreateQueueMessage msg = new CreateQueueMessage(id, queueId);
        msg.setSenderId(senderId);
        return msg;
    }
}
