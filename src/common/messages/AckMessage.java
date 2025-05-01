package common.messages;

import common.MessageType;
import common.util.MsgVisitor;

import java.util.Optional;
import java.util.UUID;

public class AckMessage extends Response {
    public AckMessage(UUID uuid) {
        super(uuid, MessageType.ACK);
    }
    @Override
    public String serialize() {
        return super.getType().name() + ":" + super.getUuid() + ":" + super.getSenderId();
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) {
        return visitor.visit(this);
    }
}
