package csx55.pastry.node;

import java.net.Socket;
import java.util.logging.*;
import csx55.pastry.wireformats.*;

public class Peer implements Node {

    private Logger log = Logger.getLogger(this.getClass().getName());
    
    private ConnInfo regConnInfo;
    private ConnInfo myConnInfo;
    public PeerInfo peerInfo;
    // Leafset
    // Routing Table
    

    public Peer(String host, int port, String hexID) {
        regConnInfo = new ConnInfo(host, port);
        peerInfo = new PeerInfo(hexID, new ConnInfo(myConnInfo.getIP(), myConnInfo.getPort()), hexID);
    }


    @Override
    public void onEvent(Event event, Socket socket) {
        
    }

    // exit method

}
