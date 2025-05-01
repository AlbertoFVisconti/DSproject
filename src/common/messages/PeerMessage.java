package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NewPeerFoundException;

import java.util.Optional;
import java.util.UUID;

public class PeerMessage extends Message {
    private final String ip;
    private final int port;

    public PeerMessage(UUID uuid, String ip, int port) {
        super(uuid, MessageType.PEER);
        this.ip = ip;
        this.port = port;
    }
    public String getIp() {
        return ip;
    }
    public int getPort() {
        return port;
    }
    @Override
    public String serialize() {
        return getType().name() + ":" + getUuid() + ":" + getSenderId() + ":" + ip + ":" + port;
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) throws NewPeerFoundException {
        return visitor.visit(this);
    }
}
