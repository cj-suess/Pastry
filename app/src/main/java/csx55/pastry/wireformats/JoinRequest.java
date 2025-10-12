package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;

public class JoinRequest extends Event {
    
    public int messageType;
    public PeerInfo peerInfo;
    private String destinationHex;

    public JoinRequest(int messageType, PeerInfo peerInfo, String myHexID) {
        this.messageType = messageType;
        this.peerInfo = peerInfo;
        this.destinationHex = myHexID;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writeString(dout, peerInfo.getHexID());
        writeString(dout, peerInfo.getIP());
        dout.writeInt(peerInfo.getPort());
        writeString(dout, destinationHex);
    }

    // longest matching prefix length
    public int longestMatchingPrefixLength(String id1, String id2) {
        for(int i = 0; i < id1.length(); i++) {
            if(id1.charAt(i) != id2.charAt(i)){ return i; }
        }
        return id1.length();
    }

    public String getDestinationHex() {
        return destinationHex;
    }


}
