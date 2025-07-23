package common.messages;

import common.MessageType;
import common.util.MsgVisitor;

import java.util.Optional;
import java.util.UUID;

public class NAckMessage extends Response {
    public String error;

    public String getError() {
        return this.error;
    }

    public void setError(String errorMsg) {
        this.error = errorMsg;
    }

    public NAckMessage(UUID uuid) {
        super(uuid, MessageType.NACK);
    }
    @Override
    public String serialize() {
        return getType().name() + ":" + getUuid() + ":" + getSenderId() + ":" + getError();
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) {
        return visitor.visit(this);
    }
}
