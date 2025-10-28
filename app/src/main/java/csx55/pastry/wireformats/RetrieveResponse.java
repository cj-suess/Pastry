package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class RetrieveResponse extends Event {

    private final int messageType;
    private final List<String> routingPath;

    public RetrieveResponse(int messageType,  List<String> routingPath) {
        this.messageType = messageType;
        this.routingPath = routingPath;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writeRouting(dout);
    }

    private void writeRouting(DataOutputStream dout) throws IOException {
        dout.writeInt(routingPath.size());
        for (String hexId : routingPath) {
            writeString(dout, hexId);
        }
    }

    public List<String> getRoutingPath() { return routingPath; }
}
