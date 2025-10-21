package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;

public class Exit extends Event {
    
    int messageType;
    private final PeerInfo exitingPeer;
    private final PeerInfo newNeighbor;

    public Exit(int messageType, PeerInfo exitingPeer, PeerInfo newNeighbor) {
        this.messageType = messageType;
        this.exitingPeer = exitingPeer;
        this.newNeighbor = newNeighbor;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writeString(dout, exitingPeer.getHexID());
        writeString(dout, exitingPeer.getIP());
        dout.writeInt(exitingPeer.getPort());

        writeString(dout, newNeighbor.getHexID());
        writeString(dout, newNeighbor.getIP());
        dout.writeInt(newNeighbor.getPort());
    }

    public PeerInfo getExitingPeer() {
        return exitingPeer;
    }

    public PeerInfo getNewNeighbor() {
        return newNeighbor;
    }
}
