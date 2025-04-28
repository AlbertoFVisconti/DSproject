package common.MessageHandlers;

import common.Messages.AckMessage;
import common.Messages.AppendValueMessage;
import common.Messages.Message;

public class AppendValueHandler implements MessageHandler<AppendValueMessage> {

    @Override
    public void handle(AppendValueMessage message) {
        // TODO implement logic here
    }

    @Override
    public Message respond(AppendValueMessage message) {
        return new AckMessage();
    }
}
