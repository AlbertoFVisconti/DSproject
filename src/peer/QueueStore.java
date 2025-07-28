package peer;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueStore {
    // This is <queueId, <value, peers>>
    private ConcurrentHashMap<String, LinkedList<Map.Entry<Integer, LinkedList<String>>>> clientQueues;
    public QueueStore() {
        clientQueues = new ConcurrentHashMap<>();
    }

    public void addQueue(String queueId) {
        if (clientQueues.containsKey(queueId)) {
            System.out.println("Already queue with id " + queueId + " exists");
            throw new IllegalArgumentException("No queue with id " + queueId + " exists");
        }
        System.out.println("Adding queue with id " + queueId);
        clientQueues.put(queueId, new LinkedList<>());
    }
    public void addValue(String queueId, String clientId, int value) throws IllegalArgumentException {
        if (!clientQueues.containsKey(queueId)) {
            System.out.println("No queue with id " + queueId + " exists");
            throw new IllegalArgumentException("No queue with id " + queueId + " exists");
        }
        System.out.println("Adding " + value + " to queue " + queueId);
        clientQueues
                .computeIfAbsent(queueId, k -> new LinkedList<>())
                .addLast(new AbstractMap.SimpleEntry<>(value, new LinkedList<>()));

    }
    public int readValue(String queueId, String clientId) throws IllegalArgumentException {
        if (!clientQueues.containsKey(queueId)) {
            throw new IllegalArgumentException("No queue with id " + queueId + " exists");
        }
        if (clientQueues.get(queueId).isEmpty()) {
            throw new IllegalArgumentException("Queue " + queueId + " is empty");
        }
        for (Map.Entry<Integer, LinkedList<String>> entry : clientQueues.get(queueId)) {
            if(!entry.getValue().contains(clientId)) {
                entry.getValue().addFirst(clientId);
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Client " + clientId + " already read all of the queue");
    }

    public int getValue(){
        int value = clientQueues.size();
        for( LinkedList<Map.Entry<Integer, LinkedList<String>>> innerList : clientQueues.values()){
            value += innerList.size();
        }
        return value;
    }
    public String serialize(ConcurrentHashMap<String, LinkedList<Map.Entry<Integer, LinkedList<String>>>> clientQ) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        String serialized;
        try {
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(clientQ);
            out.close();
            serialized = Base64.getEncoder().encodeToString(byteOut.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return serialized;
    }

    public ConcurrentHashMap<String, LinkedList<Map.Entry<Integer, LinkedList<String>>>> deserialize(String serialized) {
        byte[] data = Base64.getDecoder().decode(serialized);
        ObjectInputStream in = null;
        ConcurrentHashMap<String, LinkedList<Map.Entry<Integer, LinkedList<String>>>>map;
        try {
            in = new ObjectInputStream(new ByteArrayInputStream(data));
            map = (ConcurrentHashMap<String, LinkedList<Map.Entry<Integer, LinkedList<String>>>>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return map;

    }

    //getters and setters


    public ConcurrentHashMap<String, LinkedList<Map.Entry<Integer, LinkedList<String>>>> getClientQueues() {
        synchronized (clientQueues) {
            return clientQueues;
        }
    }
    public void setClientQueues(ConcurrentHashMap<String, LinkedList<Map.Entry<Integer, LinkedList<String>>>> clientQueues) {
        synchronized (clientQueues) {
            this.clientQueues = clientQueues;
        }
    }
}
