package common.MessageHandlers;

import common.Messages.AckMessage;
import common.Messages.Message;
import common.Messages.NAckMessage;

public class NAckHandler implements MessageHandler<NAckMessage> {
    @Override
    public void handle(NAckMessage message) {
        // TODO maybe convert this to an error message
        System.out.println("Received NACK from " + message.getSenderId());
    }
    @Override
    public Message respond(NAckMessage message) {
        return null;
    }
}
