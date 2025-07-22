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

    public void start(Peer peer) {
        this.peer = peer;
        if (peer.getRole() == Role.LEADER) {
            this.thread=new Thread(this::leaderLogic);
        } else if (peer.getRole() ==Role.FOLLOWER) {
            this.thread=new Thread(this::followerLogic);
        }
        thread.start();
    }
    public void stop(){
        try {
            this.thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Error interrupting leader/follower thread");
        }
    }
    public void update(Peer peer) {
        this.stop();
        this.start(peer);
    }
    public void followerLogic(){
        while (true) {
            synchronized (pingLock) {
                try {
                    Random random = new Random();
                    long waitTime = 2500 + random.nextInt(1001);
                    pingLock.wait(waitTime);
                    if (pingReceived) {
                        pingReceived = false;
                        System.out.println("Ping received");
                    } else {
                        System.out.println("Ping timeout - leader failure?");
                        CandidateMessage msg = new CandidateMessage(UUID.randomUUID(), peer.getValue());
                        peer.broadcast(msg.serialize(), "");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void leaderLogic(){
        while(true){
            PingMessage msg= new PingMessage(UUID.randomUUID(), peer.getId().toString());
            peer.broadcast(msg.serialize(), peer.getId().toString());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Leader thread interrupted");
                break;
            }

        }
    }
    public void receivedPing(){
        synchronized (pingLock) {
            pingReceived = true;
            System.out.println("Ping received");
        }
    }


}
