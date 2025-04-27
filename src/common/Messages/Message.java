package common.Messages;

import common.MessageType;

import java.util.UUID;

public abstract class Message {
    private final UUID uuid;
    private final MessageType type;
    private final String senderId;

    public Message(MessageType type, String senderId) {
        this.uuid = UUID.randomUUID();
        this.type = type;
        this.senderId = senderId;
    }

    public UUID getUuid() {
        return uuid;
    }
    public String getSenderId() {
        return senderId;
    }
}
