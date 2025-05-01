package common.util;

import common.messages.*;

import java.util.Optional;

public class AbstractMsgVisitor implements MsgVisitor {

    @Override
    public Optional<Response> visit(AckMessage message) {
        return Optional.empty();
    }

    @Override
    public Optional<Response> visit(NAckMessage message) {
        return Optional.empty();
    }

    @Override
    public Optional<Response> visit(AddClientMessage message) throws NotLeaderException, NewClientFoundException{
        return Optional.empty();
    }

    @Override
    public Optional<Response> visit(AppendValueMessage message) throws NotLeaderException {
        return Optional.empty();
    }
    public Optional<Response> visit(PeerMessage message) {
        return Optional.empty();
    }
}
