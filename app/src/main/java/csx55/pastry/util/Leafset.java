package csx55.pastry.util;

import java.util.TreeSet;

import csx55.pastry.wireformats.*;

public class Leafset {
    
    private TreeSet<PeerInfo> neighbors = new TreeSet<>();

    public void addPeer(PeerInfo peerInfo) {
        neighbors.add(peerInfo);
    }
    
    public PeerInfo getlower(PeerInfo peerInfo) {
        return neighbors.lower(peerInfo);
    }

    public PeerInfo getHigher(PeerInfo peerInfo) {
        return neighbors.higher(peerInfo);
    }

    public TreeSet<PeerInfo> getLeafSet() {
        return neighbors;
    }

    public boolean checkDestinationHex(String destinationHex) {
        for(PeerInfo p : neighbors){
            if (p.getHexID().equals(destinationHex)){
                return true;
            }
        }
        return false;
    }
}
