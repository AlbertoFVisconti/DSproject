package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class ReadValueMessage extends Message {
    private final String queueId;

    public ReadValueMessage(UUID uuid,  String queueId) {
        super(uuid, MessageType.READVALUE);
        this.queueId = queueId;
    }

    public String getQueueId() {
        return queueId;
    }

    @Override
    public String serialize() {
        return getType().name() + ":" + getUuid() + ":" + getSenderId() + ":" + getQueueId();
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException {
        return visitor.visit(this);
    }
}
