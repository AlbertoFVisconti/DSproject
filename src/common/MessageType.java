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
    ACK,
    NACK
}
