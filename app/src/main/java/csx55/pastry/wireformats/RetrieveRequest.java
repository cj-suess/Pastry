package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class RetrieveRequest extends Event{

    private final int messageType;
    private final PeerInfo dataNode;
    private final String fileName;
    private final List<String> routingPath;

    public RetrieveRequest(int messageType, PeerInfo peerInfo, String fileName, List<String> routingPath) {
        this.messageType = messageType;
        this.dataNode = peerInfo;
        this.fileName = fileName;
        this.routingPath = routingPath;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writePeerInfo(dout, dataNode);
        writeString(dout, fileName);
        writeRouting(dout);
    }

    private void writeRouting(DataOutputStream dout) throws IOException {
        dout.writeInt(routingPath.size());
        for (String hexId : routingPath) {
            writeString(dout, hexId);
        }
    }

    private void writePeerInfo(DataOutputStream dout, PeerInfo peerInfo) throws IOException {
        writeString(dout, peerInfo.getHexID());
        writeString(dout, peerInfo.getIP());
        dout.writeInt(peerInfo.getPort());
    }

    public PeerInfo getDataNode() { return dataNode; }
    public String  getFileName() { return fileName; }
    public List<String> getRoutingPath() { return routingPath; }
}
