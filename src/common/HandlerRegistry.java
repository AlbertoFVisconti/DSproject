package common;

import common.messageHandlers.Handler;
import common.messages.Message;
import common.messages.Response;
import common.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HandlerRegistry {
    private final Map<MessageType, Handler<?>> handlers = new HashMap<>();

    public void registerHandler(MessageType type, Handler<?> handler) {
        handlers.put(type, handler);
    }

    public Optional<Response> handle(Message message) throws NotLeaderException, NewClientFoundException, NewPeerFoundException {
        MsgVisitor handler = handlers.get(message.getType());
        if (handler == null) { throw new UnsupportedOperationException("Unsupported operation!"); }
        Optional<Response> res = message.accept(handler);
        return res;
    }

    public Message deserialize(String serializedMessage) {
        int separatorIndex = serializedMessage.indexOf(':');
        if (separatorIndex == -1) {
            throw new IllegalArgumentException("Invalid data: " + serializedMessage);
        }
        MessageType type = MessageParser.parseType(serializedMessage);
        String payload = serializedMessage.substring(separatorIndex + 1);

        Handler<?> deserializer = handlers.get(type);
        if (deserializer == null) {
            throw new IllegalArgumentException("Unknown message type: " + type);
        }
        return deserializer.deserialize(payload);
    }
}
