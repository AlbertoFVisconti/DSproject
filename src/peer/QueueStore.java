package peer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueStore {
    // This is <queueId, <clientId, value>>
    private ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>> clientQueues;
    public QueueStore() {
        clientQueues = new ConcurrentHashMap<>();
    }

    public void addQueue(String queueId) {
        if (clientQueues.containsKey(queueId)) {
            System.out.println("Already queue with id " + queueId + " exists");
            throw new IllegalArgumentException("No queue with id " + queueId + " exists");
        }
        System.out.println("Adding queue with id " + queueId);
        clientQueues.put(queueId, new ConcurrentHashMap<>());
    }
    public void addValue(String queueId, String clientId, int value) throws IllegalArgumentException {
        if (!clientQueues.containsKey(queueId)) {
            System.out.println("No queue with id " + queueId + " exists");
            throw new IllegalArgumentException("No queue with id " + queueId + " exists");
        }
        System.out.println("Adding " + value + " to queue " + queueId);
        clientQueues.computeIfAbsent(queueId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(clientId, k -> (new LinkedList<>()))
                .addFirst(value);
    }
    public int readValue(String queueId, String clientId) throws IllegalArgumentException {
        if (!clientQueues.containsKey(queueId)) {
            throw new IllegalArgumentException("No queue with id " + queueId + " exists");
        }
        if (!clientQueues.get(queueId).containsKey(clientId)) {
            throw new IllegalArgumentException("Queue " + queueId + " is empty");
        }
        return clientQueues.get(queueId).get(clientId).getFirst();
    }
    public int getValue(){
        int value = 0;
        for(ConcurrentHashMap<String, LinkedList<Integer>> innerMap : clientQueues.values()){
            value += innerMap.size();
        }
        return value;
    }
}
