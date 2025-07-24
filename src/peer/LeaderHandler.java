package peer;

import common.messages.CandidateMessage;
import common.messages.PingMessage;
import raft.Role;

import java.util.Random;
import java.util.UUID;

public class LeaderHandler {
    private Thread thread;
    private final Object pingLock=new Object();
    private boolean pingReceived = false;
    private Peer peer;
    private boolean listening = false;

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
                    long waitTime = 2500 + random.nextInt(1001);
                    pingLock.wait(waitTime);
                    if (pingReceived) {
                        pingReceived = false;
                    } else {
                        System.out.println("Ping timeout - leader failure? Candidating");
                        CandidateMessage msg = new CandidateMessage(UUID.randomUUID(), peer.getValue());
                        msg.setSenderId(peer.getId().toString());
                        peer.broadcast(msg.serialize(), "");
                        peer.setRole(Role.LEADER);
                        peer.setLeader(peer.getId().toString());
                        this.listening=false;
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


}
