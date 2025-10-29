package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;


public class RetrieveResponse extends Event {

    private final int messageType;
    private final byte[] data;
    private final List<String> routingPath;

    public RetrieveResponse(int messageType, byte[] data,  List<String> routingPath) {
        this.messageType = messageType;
        this.data = data;
        this.routingPath = routingPath;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writeData(dout);
        writeRouting(dout);
    }

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

    public List<String> getRoutingPath() { return routingPath; }
    public byte[] getData() { return data; }
}
