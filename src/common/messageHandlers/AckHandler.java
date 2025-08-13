package common.messageHandlers;

import common.messages.AckMessage;
import common.messages.Response;

import java.util.Optional;
import java.util.UUID;

public class AckHandler extends Handler<AckMessage> {
    private final Object lock;
    public AckHandler(Object lock) {
        super();
        this.lock = lock;
    }
    @Override
    public Optional<Response> visit(AckMessage message) {
        System.out.println("Received ACK from " + message.getSenderId().substring(0, 8));
        synchronized (lock) {
            lock.notifyAll();
        }
        return Optional.empty();
    }

    @Override
    public AckMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        AckMessage msg = new AckMessage(id);
        msg.setSenderId(senderId);
        return msg;
    }
}
