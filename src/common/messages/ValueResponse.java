package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class ValueResponse extends Response {
    private int value;
    public ValueResponse(UUID uuid) {
        super(uuid, MessageType.VALRES);
    }

    public int getValue() {
        return value;
    }
    public void setValue(int value) {
        this.value = value;
    }
    @Override
    public String serialize() {
        return super.getType().name() + ":" + super.getUuid() + ":" + super.getSenderId() + ":" + value;
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) {
        return visitor.visit(this);
    }
}

