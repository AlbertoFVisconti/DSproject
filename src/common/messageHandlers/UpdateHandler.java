package common.messageHandlers;

import common.messages.PingMessage;
import common.messages.Response;
import common.messages.UpdateMessage;
import peer.QueueStore;

import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateHandler extends Handler<UpdateMessage>{
    private final QueueStore queueStore;
    public UpdateHandler(QueueStore queueStore) {
        this.queueStore = queueStore;
    }
    @Override
    public UpdateMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        int value = Integer.parseInt(parts[1]);
        ConcurrentHashMap<String, LinkedList<Map.Entry<Integer, LinkedList<String>>>> map = queueStore.deserialize(parts[2]);
        UpdateMessage msg= new UpdateMessage(id,value,queueStore, map);
        msg.setSenderId(msg.getSenderId());
        return msg;
    }

    @Override
    public Optional<Response> visit(UpdateMessage message) {
        if(message.getValue()>queueStore.getValue()){
            queueStore.setClientQueues(message.getClientQueues());
            System.out.println("Client queues updated: " + queueStore.getClientQueues());
        }
        return Optional.empty();
    }
}
