package common.Messages;

import common.MessageType;

public class AddClientMessage extends Message {
    private final String ip;
    private final int port;

    public AddClientMessage(String senderId, String ip, int port) {
        super(MessageType.ADDCLIENT, senderId);
        this.ip = ip;
        this.port = port;
    }
    public String getIp() {
        return ip;
    }
    public int getPort() {
        return port;
    }
}
