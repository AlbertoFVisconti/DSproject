package common.Messages;

import common.MessageType;

public class NAckMessage extends Message {
    public NAckMessage(String senderId) {
        super(MessageType.NACK, senderId);
    }
}
