package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class AppendValueMessage extends Message {
    private final String queueId;
    private final int value;
    private final String leaderId;

    public AppendValueMessage(UUID uuid, String queueId, int value, String leaderId) {
        super(uuid, MessageType.APPENDVALUE);
        this.value = value;
        this.queueId = queueId;
        this.leaderId = leaderId;
    }

    public int getValue() {
        return value;
    }
    public String getQueueId() {
        return queueId;
    }
    public String getLeaderId() {return leaderId;}
    @Override
    public String serialize() {
        String res= getType().name() + ":" + getUuid() + ":" + getSenderId() + ":" + getQueueId() + ":" + getValue();
        if(leaderId != null) { res= res+":"+leaderId; }
        return res;

    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException {
        return visitor.visit(this);
    }
}
