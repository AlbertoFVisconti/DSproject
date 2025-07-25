package common;

public enum MessageType {
    // message is PEER:PEER_ID:PEER_IP:PEER_PORT
    // this message is used to share a new peer
    PEER,
    // message is ADD_CLIENT:CLIENT_ID:CLIENT_IP:CLIENT_PORT
    // this message is used to add a client to the client list
    ADDCLIENT,
    // message is APPENDVALUE:TOKEN:CLIENT_ID:QUEUE_ID:VALUE
    // this message is used to add a single value to a specific queue
    APPENDVALUE,
    //message is CREATEQUEUE:TOKEN:CLIENT_ID:QUEUE_ID
    //this message is used to create a single queue
    CREATEQUEUE,
    READVALUE,
    VALRES,
    //message is PING:TOKEN:LEADER_ID
    //this message is sent by the Peer Leader to prove it's alive
    PING,
    //message is CANDIDATE:TOKEN:CANDIDATE_ID:VALUE
    //this message is sent after a Leader's timeout
    //value is given by the total number of values in every queue, the peer with the highest value wins
    CANDIDATE,
    //message is UPDATE:TOKEN:VALUE:QUEUESTORE
    //this message is sent to a peer joining the network to bring it up to date with the state of the queues
    UPDATE,
    ACK,
    NACK
}
