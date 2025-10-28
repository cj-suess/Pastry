package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class StoreRequest extends Event {

    private final int messageType;
    private final PeerInfo dataNode;
    private final String fileName;
    private final byte[] data;
    private final List<String> routingPath;

    public StoreRequest(int messageType, PeerInfo dataNode, String fileName,  byte[] data, List<String> routingPath) {
        this.messageType = messageType;
        this.dataNode = dataNode;
        this.fileName = fileName;
        this.data = data;
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
        writeData(dout);
        writeRouting(dout);
    }

    public PeerInfo getDataNode() { return dataNode; }
    public byte[] getData() { return data; }
    public String  getFileName() { return fileName; }
    public List<String> getRoutingPath() { return routingPath; }

    private void writeRouting(DataOutputStream dout) throws IOException {
        dout.writeInt(routingPath.size());
        for (String hexId : routingPath) {
            writeString(dout, hexId);
        }
    }

    private void writeData(DataOutputStream dout) throws IOException {
        dout.writeInt(data.length);
        dout.write(data, 0, data.length);
    }

    private void writePeerInfo(DataOutputStream dout, PeerInfo peerInfo) throws IOException {
        writeString(dout, peerInfo.getHexID());
        writeString(dout, peerInfo.getIP());
        dout.writeInt(peerInfo.getPort());
    }
}
