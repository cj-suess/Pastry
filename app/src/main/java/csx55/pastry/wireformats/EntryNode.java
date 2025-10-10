package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;

public class EntryNode extends Event {
    
    public int messageType;
    public PeerInfo peerInfo;

    public EntryNode(int messageType, PeerInfo peerInfo) {
        this.messageType = messageType;
        this.peerInfo = peerInfo;
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
    }
}
