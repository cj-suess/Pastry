package csx55.pastry.util;

import java.util.ArrayList;
import java.util.List;
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

    public PeerInfo findClosestNeighbor(String joiningNodeHexId) {

        if(neighbors.isEmpty()){ return null; }

        PeerInfo joiningNodeInfo = new PeerInfo(joiningNodeHexId, null);
        PeerInfo lower = neighbors.floor(joiningNodeInfo);
        PeerInfo higher = neighbors.ceiling(joiningNodeInfo);

        if(lower == null) { return higher; } // if no smaller peer in ls return the higher peer
        if(higher == null || lower.equals(higher)) { return lower; } // if no higher peer in ls or lower and higher are the same then either only lower exists or an exact match was found

        long joiningVal = Long.parseLong(joiningNodeHexId, 16);
        long lowerVal = Long.parseLong(lower.getHexID(), 16);
        long higherVal = Long.parseLong(higher.getHexID(), 16);

        if(Math.abs(joiningVal - lowerVal) <= Math.abs(joiningVal - higherVal)) {
            return lower;
        } else {
            return higher;
        }
    }

    public List<PeerInfo> getAllPeers() {
        List<PeerInfo> peers = new ArrayList<>();
        for(PeerInfo peer : neighbors){
            if(peer != null) { peers.add(peer); }
        }
        return peers;
    }
}
