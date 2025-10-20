package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class Update extends Event {

    private final int messageType;
    private final PeerInfo myPeerInfo;
    private final List<PeerInfo> peers;

    public Update(int messageType, PeerInfo myPeerInfo, List<PeerInfo> peers) {
        this.messageType = messageType;
        this.myPeerInfo = myPeerInfo;
        this.peers = peers;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writePeerInfo(dout, myPeerInfo);
        dout.writeInt(peers.size());
        writePeers(dout, peers);
    }

    private void writePeers(DataOutputStream dout, List<PeerInfo> peers) throws IOException {
        for(PeerInfo p : peers){
            writePeerInfo(dout, p);
        }
    }

    private void writePeerInfo(DataOutputStream dout, PeerInfo peerInfo) throws IOException {
        writeString(dout, peerInfo.getHexID());
        writeString(dout, peerInfo.getIP());
        dout.writeInt(peerInfo.getPort());
    }

    public List<PeerInfo> getPeers() {
        return peers;
    }

    public PeerInfo getMyPeerInfo() {
        return myPeerInfo;
    }
    
}
