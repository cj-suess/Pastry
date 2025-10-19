package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class JoinResponse extends Event {

    private final int messageType;
    private final PeerInfo peerInfo;
    private final String myHexId;
    private final List<PeerInfo> leafsetList;
    private final List<PeerInfo> rtList;

    public JoinResponse(int messageType, PeerInfo peerInfo, String myHexId, List<PeerInfo> leafseList, List<PeerInfo> rtList) {
        this.messageType = messageType;
        this.peerInfo = peerInfo;
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
        writePeerInfo(dout, peerInfo);
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

    public PeerInfo getPeerInfo(){
        return peerInfo;
    }

    public List<PeerInfo> getRoutingTable(){
        return rtList;
    }

    public List<PeerInfo> getLeafset() {
        return leafsetList;
    }
    
}
