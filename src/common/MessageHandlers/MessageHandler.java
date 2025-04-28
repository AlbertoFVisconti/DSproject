package common.MessageHandlers;

import common.Messages.Message;

public interface MessageHandler<T extends Message> {
    void handle(T message);
    // This I'm not so sure of
    Message respond(T message);
}
