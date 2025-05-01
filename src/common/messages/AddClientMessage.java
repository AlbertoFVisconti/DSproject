package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class AddClientMessage extends Message {
    private final String ip;
    private final int port;

    public AddClientMessage(UUID uuid, String ip, int port) {
        super(uuid, MessageType.ADDCLIENT);
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
    public Optional<Response> accept(MsgVisitor visitor) throws NotLeaderException, NewClientFoundException {
        return visitor.visit(this);
    }
}
