package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class AppendValueMessage extends Message {
    private final String queueId;
    private final int value;

    public AppendValueMessage(UUID uuid, String queueId, int value) {
        super(uuid, MessageType.APPENDVALUE);
        this.value = value;
        this.queueId = queueId;
    }

    public int getValue() {
        return value;
    }
    public String getQueueId() {
        return queueId;
    }
    @Override
    public String serialize() {
        return getType().name() + ":" + getUuid() + ":" + getSenderId() + ":" + getQueueId() + ":" + getValue();
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException {
        return visitor.visit(this);
    }
}
