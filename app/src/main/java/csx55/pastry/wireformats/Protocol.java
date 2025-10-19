package csx55.pastry.wireformats;

public interface Protocol {
    int REGISTER_REQUEST = 1;
    int REGISTER_RESPONSE = 2;
    int DEREGISTER_REQUEST = 3;
    int DEREGISTER_RESPONSE = 4;
    int ENTRY_NODE = 5;
    int JOIN_REQUEST = 6;
    int JOIN_RESPONSE = 7;
    int NODE_ID = 8;
    int UPDATE = 9;
}
