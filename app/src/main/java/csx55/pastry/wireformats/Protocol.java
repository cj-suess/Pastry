package csx55.pastry.wireformats;

public interface Protocol {
    int REGISTER_REQUEST = 1;
    int REGISTER_RESPONSE = 2;
    int DEREGISTER_REQUEST = 3;
    int DEREGISTER_RESPONSE = 4;
    int ENTRY_NODE = 5;
    int JOIN_REQUEST = 6;
    int JOIN_RESPONSE = 7;
    int EXIT = 8;
    int REFERENCE = 9;
    int REFERENCE_NOTIFICATION = 10;
    int HANDSHAKE = 11;
    int LEAFSET_UPDATE = 12;
    int ROUTING_UPDATE = 13;
}
