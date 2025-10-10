package csx55.pastry.wireformats;

public interface Protocol {
    int REGISTER_REQUEST = 1;
    int REGISTER_RESPONSE = 2;
    int DEREGISTER_REQUEST = 3;
    int DEREGISTER_RESPONSE = 4;
    int CONN_INFO = 5;
    int ID_MESSAGE = 6;
}
