package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import csx55.pastry.util.Leafset;
import csx55.pastry.util.RoutingTable;

public class JoinResponse extends Event {

    private final int messageType;
    private final PeerInfo peerInfo;
    private final String myHexId;
    private final Leafset ls;
    private final RoutingTable rt;

    public JoinResponse(int messageType, PeerInfo peerInfo, String myHexId, Leafset ls, RoutingTable rt) {
        this.messageType = messageType;
        this.peerInfo = peerInfo;
        this.myHexId = myHexId;
        this.ls = ls;
        this.rt = rt;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writePeerInfo(dout, peerInfo);
        writeString(dout, myHexId);
        List<PeerInfo> lsList = ls.getAllPeers();
        dout.writeInt(lsList.size());
        writeLeafset(dout, lsList);
        List<PeerInfo> rtList = rt.getAllPeers();
        dout.writeInt(rtList.size());
        writeRoutingTable(dout, rt);
    }

    private void writeLeafset(DataOutputStream dout, List<PeerInfo> ls) throws IOException {
        for(PeerInfo p : ls){
            writePeerInfo(dout, p);
        }
    }

    private void writeRoutingTable(DataOutputStream dout, RoutingTable rt) throws IOException {
        for(int i = 0; i < 4; i++){
            for(int j = 0; j < 16; j++) {
                if(rt.getPeerInfo(i,j) != null){
                    dout.writeInt(i);
                    dout.writeInt(j);
                    writePeerInfo(dout, rt.getPeerInfo(i, j));
                }
            }
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

    public RoutingTable getRoutingTable(){
        return rt;
    }

    public Leafset getLeafset() {
        return ls;
    }
    
}
