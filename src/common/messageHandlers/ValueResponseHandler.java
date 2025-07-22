package common.messageHandlers;

import common.messages.AckMessage;
import common.messages.Response;
import common.messages.ValueResponse;

import java.util.Optional;
import java.util.UUID;

public class ValueResponseHandler extends Handler<ValueResponse> {
    private final Object lock;
    public ValueResponseHandler(Object lock) {
        super();
        this.lock = lock;
    }
    @Override
    public Optional<Response> visit(ValueResponse message) {
        System.out.println(message.getValue());
        synchronized (lock) {
            lock.notifyAll();
        }
        return Optional.empty();
    }

    @Override
    public ValueResponse deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        int value = Integer.parseInt(parts[2]);
        ValueResponse msg = new ValueResponse(id);
        msg.setSenderId(senderId);
        msg.setValue(value);
        return msg;
    }
}

