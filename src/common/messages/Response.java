package common.messages;

import common.MessageType;

import java.util.UUID;

public abstract class Response extends Message {
    public Response(UUID uuid, MessageType type) {
        super(uuid, type);
    }
}
