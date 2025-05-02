package common.messageHandlers;

import common.messages.AckMessage;
import common.messages.AddClientMessage;
import common.messages.Response;
import common.util.NewClientFoundException;
import common.util.NotLeaderException;
import peer.AddressRegistry;
import raft.Role;

import java.util.Optional;
import java.util.UUID;

public class AddClientHandler extends Handler<AddClientMessage> {
    private final AddressRegistry clientRegistry;
    private Role recvRole;

    public AddClientHandler(AddressRegistry clientRegistry, Role role) {
        this.clientRegistry = clientRegistry;
        this.recvRole = role;
    }

    @Override
    public Optional<Response> visit(AddClientMessage message) throws NotLeaderException, NewClientFoundException {
        if (recvRole != Role.LEADER) {
            throw new NotLeaderException("Peer is not leader.");
        }
        String id = message.getSenderId();
        String ip = message.getIp();
        int port = message.getPort();
        if (clientRegistry.getAddress(id) == null) {
            clientRegistry.addEntry(id, ip + ":" + port);
            throw new NewClientFoundException("New client " + id + " found. Forwarding to all peers.");
        }
        clientRegistry.addEntry(id, ip + ":" + port);
        return Optional.of(new AckMessage(message.getUuid()));
    }

    @Override
    public AddClientMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        String ip = parts[2];
        int port = Integer.parseInt(parts[3]);
        AddClientMessage msg = new AddClientMessage(id, ip, port);
        msg.setSenderId(senderId);
        return msg;
    }

    public Role getRecvRole() {
        return recvRole;
    }

    public void setRecvRole(Role recvRole) {
        this.recvRole = recvRole;
    }
}
