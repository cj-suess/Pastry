package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;

public class LeafsetUpdate extends Event {

    public final static int LOWER = 0;
    public final static int HIGHER = 1;

    private final int messageType;
    private final PeerInfo peerInfo;
    private final int leafsetRole;

    public LeafsetUpdate(int messageType, PeerInfo peerInfo, int leafsetRole) {
        this.messageType = messageType;
        this.peerInfo = peerInfo;
        this.leafsetRole = leafsetRole;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writePeerInfo(dout, peerInfo);
        dout.writeInt(leafsetRole);
    }

    public PeerInfo getPeerInfo() { return peerInfo; }
    
    public int getRole() { return leafsetRole; }

    private void writePeerInfo(DataOutputStream dout, PeerInfo peerInfo) throws IOException {
        writeString(dout, peerInfo.getHexID());
        writeString(dout, peerInfo.getIP());
        dout.writeInt(peerInfo.getPort());
    }
    
}
