package common.messages;

import common.MessageType;
import common.util.MsgVisitor;

import java.util.Optional;
import java.util.UUID;

public class NAckMessage extends Response {
    public NAckMessage(UUID uuid) {
        super(uuid, MessageType.NACK);
    }
    @Override
    public String serialize() {
        return getType().name() + ":" + getUuid() + ":" + getSenderId();
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) {
        return visitor.visit(this);
    }
}
