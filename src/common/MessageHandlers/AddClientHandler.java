package common.MessageHandlers;

import common.Messages.AckMessage;
import common.Messages.AddClientMessage;
import common.Messages.Message;
import peer.AddressRegistry;

public class AddClientHandler implements MessageHandler<AddClientMessage> {
    private AddressRegistry clientRegistry;

    public AddClientHandler(AddressRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
    }

    @Override
    public void handle(AddClientMessage message) {
        String id = message.getSenderId();
        String ip = message.getIp();
        int port = message.getPort();
        clientRegistry.addEntry(id, ip + ":" + port);
    }

    @Override
    public Message respond(AddClientMessage message) {
        return new AckMessage();
    }
}
