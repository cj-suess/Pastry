package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;

public class Exit extends Event {

    public static final int LOWER = 0;
    public static final int HIGHER = 1;
    
    int messageType;
    private final PeerInfo exitingPeer;
    private final PeerInfo newNeighbor;
    private final int role;

    public Exit(int messageType, PeerInfo exitingPeer, PeerInfo newNeighbor, int role) {
        this.messageType = messageType;
        this.exitingPeer = exitingPeer;
        this.newNeighbor = newNeighbor;
        this.role = role;
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

        dout.writeInt(role);
    }

    public PeerInfo getExitingPeer() {
        return exitingPeer;
    }

    public PeerInfo getNewNeighbor() {
        return newNeighbor;
    }

    public int getRole() {
        return role;
    }
}
