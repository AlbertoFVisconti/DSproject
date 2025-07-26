package peer;

import common.messages.CandidateMessage;
import common.messages.PingMessage;
import common.messages.UpdateMessage;
import raft.Role;

import java.util.Random;
import java.util.UUID;

public class LeaderHandler {
    private Thread thread;
    private final Object pingLock=new Object();
    private boolean pingReceived = false;
    private Peer peer;
    private boolean listening = false;
    private int counter=0;

    public void start(Peer peer) {
        this.peer = peer;
        this.listening = true;
        if (peer.getRole() == Role.LEADER) {
            this.thread=new Thread(this::leaderLogic);
        } else if (peer.getRole() ==Role.FOLLOWER) {
            this.thread=new Thread(this::followerLogic);
        }
        thread.start();
    }
    public void stop(){
        try {
            this.listening = false;
            this.thread.interrupt();
            this.thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Error interrupting leader/follower thread");
        }
    }

    public void followerLogic(){
        while (listening) {
            synchronized (pingLock) {
                try {
                    Random random = new Random();
                    long waitTime = 2500+ random.nextInt(1001);
                    pingLock.wait(waitTime);
                    if (pingReceived) {
                        pingReceived = false;
                    } else {
                        this.listening=false;
                        this.peer.setLeader(null);
                        System.out.println("Ping timeout - leader failure? Candidating");
                        CandidateMessage msg = new CandidateMessage(UUID.randomUUID(), peer.getValue());
                        msg.setSenderId(peer.getId().toString());
                        peer.broadcast(msg.serialize(), "");
                        peer.setRole(Role.LEADER);
                        peer.setLeader(peer.getId().toString());
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void leaderLogic(){
        while(listening){
            PingMessage msg= new PingMessage(UUID.randomUUID(), peer.getId().toString());
            peer.broadcast(msg.serialize(), peer.getId().toString());
            try {
                Thread.sleep(500);
                counter++;
                if(counter==3){
                    UpdateMessage updateMessage= new UpdateMessage(UUID.randomUUID(), peer.getValue(), peer.getQueueStore(),peer.getQueueStore().getClientQueues());
                    peer.broadcast(updateMessage.serialize(),this.peer.getId().toString());
                    counter=0;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

        }
    }
    public void receivedPing(){
        synchronized (pingLock) {
            pingReceived = true;
        }
    }
    public void setLeader(String leader_id){
        this.peer.setLeader(leader_id);
    }

    public boolean isListening() {
        synchronized (pingLock) {return listening;}
    }
}
