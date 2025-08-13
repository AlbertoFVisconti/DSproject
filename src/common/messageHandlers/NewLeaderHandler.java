package common.messageHandlers;

import common.messages.AckMessage;
import common.messages.NewLeaderMessage;
import common.messages.Response;
import common.util.NewLeaderException;

import javax.net.ssl.HandshakeCompletedEvent;
import java.util.Optional;
import java.util.UUID;

public class NewLeaderHandler extends Handler<NewLeaderMessage> {
    String leaderIp;
    int leaderPort;

    public NewLeaderHandler(String leaderIp, int leaderPort) {
        super();
        this.leaderIp = leaderIp;
        this.leaderPort = leaderPort;
    }

    @Override
    public NewLeaderMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        String leaderIp = parts[2];
        int leaderPort = Integer.parseInt(parts[3]);
        NewLeaderMessage msg = new NewLeaderMessage(id);
        msg.setSenderId(senderId);
        msg.setLeaderIp(leaderIp);
        msg.setLeaderPort(leaderPort);
        return msg;
    }

    @Override
    public Optional<Response> visit(NewLeaderMessage message) throws NewLeaderException {
        throw new NewLeaderException("New Leader is: " + message.getSenderId().substring(0,8) + " at " + message.getLeaderIp() + ":" + message.getLeaderPort());
    }
}
