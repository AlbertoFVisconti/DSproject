package common.util;

import common.messages.*;

import java.util.Optional;

public interface MsgVisitor {
    Optional<Response> visit(AckMessage message);
    Optional<Response> visit(NAckMessage message);
    Optional<Response> visit(AddClientMessage message) throws NotLeaderException, NewClientFoundException;
    Optional<Response> visit(AppendValueMessage message) throws NotLeaderException;
    Optional<Response> visit(PeerMessage message);
    Optional<Response> visit(CreateQueueMessage message) throws NotLeaderException;
    Optional<Response> visit(PingMessage message);
    Optional<Response> visit(CandidateMessage message);
    Optional<Response> visit(ReadValueMessage message) throws NotLeaderException;
    Optional<Response> visit(ValueResponse message);
    Optional<Response> visit(UpdateMessage message);
}
