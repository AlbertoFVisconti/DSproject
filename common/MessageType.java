package common;

public enum MessageType {
    // message is PEER:PEER_ID:PEER_IP:PEER_PORT
    // this message is used to share a new peer
    PEER,
    // message is ADD:CLIENT_ID:QUEUE_ID:VALUE:TOKEN
    // this message is used to add a single value to a specific queue
    ADD,
    // message is ADD:CLIENT_ID:CLIENT_IP:CLIENT_PORT
    // this message si used to add a client to the client list
    ADDCLIENT,
    // message is APPENDVALUE:CLIENT_ID:QUEUE_ID:VALUE:TOKEN
    // this message is used to add a single value to a specific queue
    APPENDVALUE
}
