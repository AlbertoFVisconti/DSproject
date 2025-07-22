package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class CandidateMessage extends  Message {
    private int value;
    public CandidateMessage(UUID uuid, int value){
        super(uuid, MessageType.CANDIDATE);
        this.value = value;
    }
    @Override
    public String serialize() {
        return getType().name() + ":" + getUuid().toString()+":"+getSenderId() + ":" + value;
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException, NewClientFoundException, NewPeerFoundException {
        return visitor.visit(this);
    }

    public int getValue() {
        return value;
    }
}
