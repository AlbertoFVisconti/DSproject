package peer;

import java.io.*;
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
        clientQueues.get(queueId)
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
        int value = clientQueues.size();
        for(ConcurrentHashMap<String, LinkedList<Integer>> innerMap : clientQueues.values()){
            value += innerMap.size();
        }
        return value;
    }
    public String serialize(ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>> clientQ) {
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

    public ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>> deserialize(String serialized) {
        byte[] data = Base64.getDecoder().decode(serialized);
        ObjectInputStream in = null;
        ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>> map;
        try {
            in = new ObjectInputStream(new ByteArrayInputStream(data));
            map = (ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return map;

    }

    //getters and setters


    public ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>> getClientQueues() {
        synchronized (clientQueues) {
            return clientQueues;
        }
    }
    public void setClientQueues(ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Integer>>> clientQueues) {
        synchronized (clientQueues) {
            this.clientQueues = clientQueues;
        }
    }
}
