package peer;

import java.util.concurrent.ConcurrentHashMap;

public class AddressRegistry {
    private ConcurrentHashMap<String, String> idToAddress;
    public AddressRegistry() {
        this.idToAddress = new ConcurrentHashMap<>();
    }

    public void addEntry(String id, String address) {
        idToAddress.put(id, address);
    }
    public void removeEntry(String id) {
        idToAddress.remove(id);
    }
    public String getAddress(String id) {
        return idToAddress.get(id);
    }
    public ConcurrentHashMap.KeySetView<String, String> getIds() {return idToAddress.keySet(); }
}
