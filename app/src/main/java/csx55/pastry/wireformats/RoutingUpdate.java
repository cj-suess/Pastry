package csx55.pastry.wireformats;

import java.util.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class RoutingUpdate extends Event {

    private final int messageType;
    private final List<PeerInfo> routingList;

    public RoutingUpdate(int messageType, List<PeerInfo> routingList) {
        this.messageType = messageType;
        this.routingList = routingList;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        dout.writeInt(routingList.size());
        writeRoutingTable(dout, routingList);
    }

    private void writePeerInfo(DataOutputStream dout, PeerInfo peerInfo) throws IOException {
        writeString(dout, peerInfo.getHexID());
        writeString(dout, peerInfo.getIP());
        dout.writeInt(peerInfo.getPort());
    }

    private void writeRoutingTable(DataOutputStream dout, List<PeerInfo> rt) throws IOException {
        for(PeerInfo p : rt){
            writePeerInfo(dout, p);
        }
    }

    public List<PeerInfo> getRoutingList() { return routingList; }
}
