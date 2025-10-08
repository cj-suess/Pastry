package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;

public class Register extends Event {

    public int messageType;
    public PeerInfo peerInfo;

    public Register(int messageType, PeerInfo peerInfo) {
        this.messageType = messageType;
        this.peerInfo = peerInfo;
    }

    @Override
    public int getType() {
        return Protocol.REGISTER_REQUEST;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writeString(dout, peerInfo.getHexID());
        writeString(dout, peerInfo.getIP());
        dout.writeInt(peerInfo.getPort());
    }
}
