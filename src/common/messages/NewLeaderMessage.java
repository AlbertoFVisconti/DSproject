package common.messages;

import common.MessageType;
import common.util.MsgVisitor;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;

import java.util.Optional;
import java.util.UUID;

public class NewLeaderMessage extends Response {
    private String leaderIp;
    private int leaderPort;

    public NewLeaderMessage(UUID uuid) {
        super(uuid, MessageType.NEWLEADER);
    }

    @Override
    public String serialize() {
        return super.getType().name() + ":" + super.getUuid() + ":" + super.getSenderId() + ":" + leaderIp + ":" + leaderPort;
    }

    @Override
    public Optional<Response> accept(MsgVisitor visitor) {
        return visitor.visit(this);
    }

    public String getLeaderIp() {
        return leaderIp;
    }
    public void setLeaderIp(String leaderIp) {
        this.leaderIp = leaderIp;
    }
    public int getLeaderPort() {
        return leaderPort;
    }
    public void setLeaderPort(int leaderPort) {
        this.leaderPort = leaderPort;
    }
}
