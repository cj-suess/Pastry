package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class StoreRequest extends Event {

    private final int messageType;
    private final String fileHex;
    private final byte[] data;
    private final List<String> routingPath;

    public StoreRequest(int messageType, String fileHex,  byte[] data, List<String> routingPath) {
        this.messageType = messageType;
        this.fileHex = fileHex;
        this.data = data;
        this.routingPath = routingPath;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writeString(dout, fileHex);
        writeData(dout);
        writeRouting(dout);
    }

    public byte[] getData() { return data; }

    public String  getFileHex() { return fileHex; }

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
}
