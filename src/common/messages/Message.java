package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class Message implements Serializable {
    private final UUID uuid;
    private final MessageType type;
    private String senderId;

    public Message(UUID uuid, MessageType type) {
        this.uuid = uuid;
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }
    public UUID getUuid() {
        return uuid;
    }
    public String getSenderId() {
        return senderId;
    }
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    public abstract String serialize();
    public abstract Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException, NewClientFoundException, NewPeerFoundException;
    // Override equals method so two messages are considered the same if they have the same UUID, senderId and type
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message msg = (Message) o;
        return Objects.equals(uuid, msg.uuid) && Objects.equals(senderId, msg.senderId) && type == msg.type;
    }
}
