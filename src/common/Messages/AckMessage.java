package common.Messages;

import common.MessageType;

public class AckMessage extends Message {
    AckMessage(String senderId) {
        super(MessageType.ACK, senderId);
    }
}
