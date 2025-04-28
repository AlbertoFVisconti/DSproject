package peer;

import java.util.concurrent.ConcurrentHashMap;

public class AddressRegistry {
    private ConcurrentHashMap<String, String> clients;
    public AddressRegistry() {
        clients = new ConcurrentHashMap<>();
    }

    public void addEntry(String clientID, String clientAddress) {
        clients.put(clientID, clientAddress);
    }
    public void removeEntry(String clientID) {
        clients.remove(clientID);
    }
    public String getAddress(String clientID) {
        return clients.get(clientID);
    }
}
