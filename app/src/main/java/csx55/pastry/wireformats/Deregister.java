package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;

public class Deregister extends Event {

    public int messageType;
    public ConnInfo connInfo;

    public Deregister(int messageType, ConnInfo connInfo) {
        this.messageType = messageType;
        this.connInfo = connInfo;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writeString(dout, connInfo.getIP());
        dout.writeInt(connInfo.getPort());
    }
    
}