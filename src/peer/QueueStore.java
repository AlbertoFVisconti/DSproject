package peer;

import java.util.concurrent.ConcurrentHashMap;

public class QueueStore {
    // This is <queueId, <clientId, value>>
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> clientQueues;
    public QueueStore() {
        clientQueues = new ConcurrentHashMap<>();
    }

    public void addQueue(String queue) {
        clientQueues.put(queue, new ConcurrentHashMap<>());
    }
    public void addValue(String queueId, String clientId, int value) throws IllegalArgumentException {
        System.out.println("Adding " + value + " to queue " + queueId);
        if (!clientQueues.containsKey(queueId)) {
            throw new IllegalArgumentException("No queue with id " + queueId + " exists");
        }
        clientQueues.get(queueId).put(clientId, value);
    }

    public int readValue(String queueId, String clientId) throws IllegalArgumentException {
        if (!clientQueues.containsKey(queueId)) {
            throw new IllegalArgumentException("No queue with id " + queueId + " exists");
        }
        return clientQueues.get(queueId).get(clientId);
    }
}
