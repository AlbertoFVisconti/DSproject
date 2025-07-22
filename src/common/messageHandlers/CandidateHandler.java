package common.messageHandlers;

import common.messages.CandidateMessage;
import common.messages.CreateQueueMessage;
import common.messages.Response;
import common.util.NewClientFoundException;
import common.util.NewPeerFoundException;
import common.util.NotLeaderException;
import peer.LeaderHandler;
import peer.Peer;
import raft.Role;

import java.util.Optional;
import java.util.UUID;

public class CandidateHandler extends  Handler<CandidateMessage>{
    private boolean received=false;
    private final Peer peer;
    private int highest=-1;
    private boolean possibleLeader=false;
    private final Object lock=new Object();

    private void Timer() {
        try {
            Thread.sleep(3000);
            synchronized (lock){
                if(possibleLeader){
                    peer.setRole(Role.LEADER);
                    peer.setLeader(peer.getId().toString());
                }
                this.highest=-1;
                this.received=false;
                this.possibleLeader=false;
                peer.getLeaderHandler().update(peer);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public CandidateHandler(Peer peer){
        this.peer = peer;
    }

    public Optional<Response> visit(CandidateMessage msg){
        if(!received){
            received=true;
            new Thread(this::Timer).start();
            if(msg.getValue()< peer.getValue()){
                CandidateMessage cmsg = new CandidateMessage(UUID.randomUUID(), peer.getValue());
                peer.broadcast(cmsg.serialize(), "");
                synchronized (lock) {this.possibleLeader=true;}
                System.out.println("Candidating as a Leader");
                return Optional.empty();
            }
        }
        if((msg.getValue()==peer.getValue() && msg.getSenderId().compareTo(peer.getId().toString()) >= 0)
        || (msg.getValue()> peer.getValue() && msg.getValue()> highest)){
            highest=msg.getValue();
            peer.setRole(Role.FOLLOWER);
            peer.setLeader(msg.getSenderId());
            synchronized(lock){
                this.possibleLeader=false;
            }
        }



        return Optional.empty();
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
