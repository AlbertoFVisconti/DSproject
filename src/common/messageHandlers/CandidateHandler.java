package common.messageHandlers;

import common.messages.*;
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
            peer.getLeaderHandler().stop();
            Thread.sleep(3000);
            synchronized (lock){
                if(possibleLeader){
                    peer.setRole(Role.LEADER);
                    peer.setLeader(peer.getId().toString());
                }
                System.out.println("Becoming: "+peer.getRole()+". Leader is: "+peer.getLeader());
                this.highest=-1;
                this.received=false;
                this.possibleLeader=false;
                peer.getLeaderHandler().start(peer);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public CandidateHandler(Peer peer){
        this.peer = peer;
    }

    public Optional<Response> visit(CandidateMessage msg){
        System.out.println("Candidate message received");
        System.out.println(peer.getId()+": "+msg.serialize());
        System.out.println(msg.getSenderId().compareTo(peer.getId().toString()));
        System.out.println(msg.getValue()+":" +peer.getValue());
        if(!received){
            received=true;
            new Thread(this::Timer).start();
            if(msg.getValue()< peer.getValue()){
                CandidateMessage cmsg = new CandidateMessage(msg.getUuid(), peer.getValue());
                cmsg.setSenderId(this.peer.getId().toString());
                peer.broadcast(cmsg.serialize(), "");
                synchronized (lock) {this.possibleLeader=true;}
                System.out.println("Candidating as a Leader");
                UpdateMessage updateMessage=new UpdateMessage(this.peer.getId(), peer.getValue(), peer.getQueueStore(), peer.getQueueStore().getClientQueues());
                updateMessage.setSenderId(this.peer.getId().toString());
                peer.broadcast(updateMessage.serialize(), "");
                return Optional.empty();
            }
        }
        if((msg.getValue()==peer.getValue() && msg.getSenderId().compareTo(peer.getId().toString()) > 0)
        || (msg.getValue()> peer.getValue() && msg.getValue()> highest)){
            synchronized(lock){
                System.out.println("becoming follower");
                highest=msg.getValue();
                peer.setRole(Role.FOLLOWER);
                peer.setLeader(msg.getSenderId());
                this.possibleLeader=false;
                //ask for update with peer message if not up to date
                PeerMessage peerMessage=new PeerMessage(this.peer.getId(), peer.getIp(),peer.getPort());
                peerMessage.setSenderId(this.peer.getId().toString());
                peer.contactPeer(msg.getSenderId(),peerMessage);
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
