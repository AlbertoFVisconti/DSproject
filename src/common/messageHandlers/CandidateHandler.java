package common.messageHandlers;

import common.messages.*;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;
import peer.LeaderHandler;
import peer.Peer;
import raft.Role;

import java.util.*;

public class CandidateHandler extends  Handler<CandidateMessage>{
    private boolean received=false;
    private final Peer peer;
    private final List<CandidateMessage> candidates=new ArrayList<>();
    private boolean election;

    private void Timer() {
        try {
            peer.getLeaderHandler().stop();
            Thread.sleep(3000);
            synchronized (candidates) {
                this.election=false;
                this.received=false;
                Message best = Collections.max(candidates, Comparator
                        .comparingInt(CandidateMessage::getValue)
                        .thenComparing(Message::getSenderId));
                this.peer.setLeader(best.getSenderId());
                if(this.peer.getId().toString().compareTo(best.getSenderId()) == 0){ this.peer.setRole(Role.LEADER);}
                else this.peer.setRole(Role.FOLLOWER);
                this.candidates.clear();
                this.peer.getLeaderHandler().start(this.peer);
                if(this.peer.getLeader().equals(this.peer.getId().toString())) {
                    System.out.println("I am the new leader!");
                    NewLeaderMessage msg = new NewLeaderMessage(UUID.randomUUID());
                    msg.setSenderId(this.peer.getId().toString());
                    msg.setLeaderIp(this.peer.getIp());
                    msg.setLeaderPort(this.peer.getPort());
                    for(String clientId : peer.getClientAddressRegistry().getIds()) {
                        peer.contactClient(clientId, msg);
                    }
                } else {
                    System.out.println(this.peer.getRole() + ": " + "leader is " + this.peer.getLeader().substring(0, 8));
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public CandidateHandler(Peer peer){
        this.peer = peer;
    }

    public Optional<Response> visit(CandidateMessage msg){
        if (candidates.contains(msg)){
            // drop message if duplicate comes around
            return Optional.empty();
        } else {
            if (!received) {
                received = true;
                this.election = true;
                new Thread(this::Timer).start();
                if (msg.getValue() < peer.getValue()) {
                    CandidateMessage cmsg = new CandidateMessage(msg.getUuid(), peer.getValue());
                    cmsg.setSenderId(this.peer.getId().toString());
                    peer.broadcast(cmsg.serialize(), "");
                    UpdateMessage updateMessage = new UpdateMessage(this.peer.getId(), peer.getValue(), peer.getQueueStore(), peer.getQueueStore().getClientQueues());
                    updateMessage.setSenderId(this.peer.getId().toString());
                    peer.broadcast(updateMessage.serialize(), "");
                    return Optional.empty();
                }
            }
            synchronized (candidates) {
                if (election) {
                    candidates.add(msg);
                }
            }
            if (msg.getValue() > peer.getValue()) {
                //ask for update with peer message if not up to date
                PeerMessage peerMessage = new PeerMessage(this.peer.getId(), peer.getIp(), peer.getPort());
                peerMessage.setSenderId(this.peer.getId().toString());
                peer.contactPeer(msg.getSenderId(), peerMessage);
            }
            return Optional.empty();
        }
    }

    @Override
    public CandidateMessage deserialize(String payload) {
        String[] parts = payload.split(":");
        UUID id = UUID.fromString(parts[0]);
        String senderId = parts[1];
        int value = Integer.parseInt(parts[2]);
        CandidateMessage msg= new CandidateMessage(id,value);
        msg.setSenderId(senderId);
        return msg;
    }
}
