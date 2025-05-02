package common.messageHandlers;

import common.messages.NAckMessage;
import common.messages.Response;

import java.util.Optional;
import java.util.UUID;

public class NAckHandler extends Handler<NAckMessage> {
    private final Object lock;
    public NAckHandler(Object lock) {
        super();
        this.lock = lock;
    }
    @Override
    public Optional<Response> visit(NAckMessage message) {
        // TODO maybe convert this to an error message
        System.out.println("Received NACK from " + message.getSenderId());
        synchronized (lock) {
            lock.notifyAll();
        }
        return Optional.empty();
    }

    @Override
    public NAckMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        NAckMessage msg = new NAckMessage(id);
        msg.setSenderId(senderId);
        return msg;
    }
}
