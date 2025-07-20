package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class CreateQueueMessage extends Message{
    private final String queueId;

    public CreateQueueMessage(UUID uuid, String queueId) {
        super(uuid, MessageType.CREATEQUEUE);
        this.queueId = queueId;
    }
    public String getQueueId() {
        return queueId;
    }
    @Override
    public String serialize() {
        return getType().name() + ":" + getUuid() + ":" + getSenderId() + ":" + getQueueId() ;
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException, NewClientFoundException, NewPeerFoundException {
        return visitor.visit(this);
    }
}
