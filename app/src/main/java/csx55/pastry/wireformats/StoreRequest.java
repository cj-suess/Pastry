package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class StoreRequest extends Event {

    private final int messageType;
    private final String fileHex;
    private byte[] data;
    private List<PeerInfo> routingPath;

    public StoreRequest(int messageType, String fileHex,  byte[] data) {
        routingPath = new ArrayList<>();
        this.messageType = messageType;
        this.fileHex = fileHex;
        this.data = data;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writeString(dout, fileHex);
        writeData(dout);
        dout.writeInt(routingPath.size());
        writeRouting(dout);
    }

    public String  getFileHex() {
        return fileHex;
    }

    public List<PeerInfo> getRoutingPath() {
        return routingPath;
    }

    private void writeRouting(DataOutputStream dout) throws IOException {
        dout.writeInt(routingPath.size());
        for (PeerInfo peerInfo : routingPath) {
            writePeerInfo(dout, peerInfo);
        }
    }

    private void writePeerInfo(DataOutputStream dout, PeerInfo peerInfo) throws IOException {
        writeString(dout, peerInfo.getHexID());
        writeString(dout, peerInfo.getIP());
        dout.writeInt(peerInfo.getPort());
    }

    private void writeData(DataOutputStream dout) throws IOException {
        dout.writeInt(data.length);
        dout.write(data, 0, data.length);
    }
}
