package common.Messages;

import common.MessageType;

public class AppendValueMessage extends Message {
    private final int value;

    public AppendValueMessage(String senderId, int value) {
        super(MessageType.APPENDVALUE);
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
