package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class ReadValueMessage extends Message {
    private final String queueId;
    private String leaderId;
    public ReadValueMessage(UUID uuid,  String queueId, String leaderId) {
        super(uuid, MessageType.READVALUE);
        this.queueId = queueId;
        this.leaderId = leaderId;
    }

    public String getQueueId() {
        return queueId;
    }
    public String getLeaderId() {return leaderId;}
    @Override
    public String serialize() {
        String res =getType().name() + ":" + getUuid() + ":" + getSenderId() + ":" + getQueueId();
        if(leaderId != null) {
            res += ":" + leaderId;
        }
        return res;
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException {
        return visitor.visit(this);
    }
}
