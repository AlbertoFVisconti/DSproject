package common.MessageHandlers;

import common.Messages.AckMessage;
import common.Messages.Message;

public class AckHandler implements MessageHandler<AckMessage> {
    @Override
    public void handle(AckMessage message) {
        System.out.println("Received ACK from " + message.getSenderId());
    }
    @Override
    public Message respond(AckMessage message) {
        return null;
    }
}
