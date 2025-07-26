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
    private String leaderId;

    public CreateQueueMessage(UUID uuid, String queueId, String leader_id) {
        super(uuid, MessageType.CREATEQUEUE);
        this.queueId = queueId;
        this.leaderId = leader_id;
    }
    public String getQueueId() {
        return queueId;
    }
    public String getLeaderId() {return leaderId;}
    @Override
    public String serialize() {
        String res=getType().name() + ":" + getUuid() + ":" + getSenderId() + ":" + getQueueId();
        if(leaderId != null) { res= res+":"+leaderId; }
        return res;
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException, NewClientFoundException, NewPeerFoundException {
        return visitor.visit(this);
    }
}
