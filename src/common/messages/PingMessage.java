package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class PingMessage extends Message{
    private final String leader_uuid;
    public PingMessage(UUID uuid,String leader_id) {
        super(uuid, MessageType.PING);
        this.leader_uuid = leader_id;
    }
    @Override
    public String serialize() {
        return getType().name() + ":" + getUuid() + ":" + getLeader_uuid() ;
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException, NewClientFoundException, NewPeerFoundException {
        return visitor.visit(this);
    }

    public String getLeader_uuid() {
        return leader_uuid;
    }
}
