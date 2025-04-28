package common.Messages;

import common.MessageType;

public class NAckMessage extends Message {
    public NAckMessage() {
        super(MessageType.NACK);
    }
}
