package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;
import peer.QueueStore;

import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateMessage extends Message {
    private QueueStore queueStore;
    private int value;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>> clientQueues;
    public UpdateMessage(UUID uuid,int value, QueueStore queueStore, ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>> clientQueues) {
        super(uuid, MessageType.UPDATE);
        this.queueStore = queueStore;
        this.clientQueues = clientQueues;
        this.value=value;

    }

    @Override
    public String serialize() {
        return getType().name()+":"+getUuid().toString()+":"+value+":"+queueStore.serialize(clientQueues);
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException, NewClientFoundException, NewPeerFoundException {
        return visitor.visit(this);
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>> getClientQueues() {
        return clientQueues;
    }

    public int getValue() {
        return value;
    }
}
