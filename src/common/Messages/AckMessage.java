package common.Messages;

import common.MessageType;

public class AckMessage extends Message {
    public AckMessage() {
        super(MessageType.ACK);
    }
}
