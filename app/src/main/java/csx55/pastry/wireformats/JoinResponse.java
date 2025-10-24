package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class JoinResponse extends Event {

    public static final int LOWER = 0;
    public static final int HIGHER = 1;

    private final int messageType;
    private final PeerInfo respondingPeer;
    private final int responderRole;
    private final String myHexId;
    private final List<PeerInfo> leafsetList;
    private final List<PeerInfo> rtList;

    public JoinResponse(int messageType, PeerInfo peerInfo, int responderRole, String myHexId, List<PeerInfo> leafseList, List<PeerInfo> rtList) {
        this.messageType = messageType;
        this.respondingPeer = peerInfo;
        this.responderRole = responderRole;
        this.myHexId = myHexId;
        this.leafsetList = leafseList;
        this.rtList = rtList;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writePeerInfo(dout, respondingPeer);
        dout.writeInt(responderRole);
        writeString(dout, myHexId);
        dout.writeInt(leafsetList.size());
        writeLeafset(dout, leafsetList);
        dout.writeInt(rtList.size());
        writeRoutingTable(dout, rtList);
    }

    private void writeLeafset(DataOutputStream dout, List<PeerInfo> ls) throws IOException {
        for(PeerInfo p : ls){
            writePeerInfo(dout, p);
        }
    }

    private void writeRoutingTable(DataOutputStream dout, List<PeerInfo> rt) throws IOException {
        for(PeerInfo p : rt){
            writePeerInfo(dout, p);
        } 
    }

    private void writePeerInfo(DataOutputStream dout, PeerInfo peerInfo) throws IOException {
        writeString(dout, peerInfo.getHexID());
        writeString(dout, peerInfo.getIP());
        dout.writeInt(peerInfo.getPort());
    }

    public PeerInfo getRespondingPeer(){ return respondingPeer; }

    public List<PeerInfo> getRoutingTable(){ return rtList; }

    public List<PeerInfo> getLeafset() { return leafsetList; }
    
    public int getResponderRole() { return responderRole; }
    
}
