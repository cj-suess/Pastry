package csx55.pastry.util;

import java.util.ArrayList;
import java.util.List;
import csx55.pastry.wireformats.*;

public class Leafset {

    private PeerInfo lower;
    private PeerInfo higher;

    public void addPeer(PeerInfo joiningPeer, String myHexId) {
        if(joiningPeer == null || joiningPeer.getHexID().equals(myHexId)) {
            return;
        }

        long myVal = Long.parseLong(myHexId, 16);
        long joiningVal = Long.parseLong(joiningPeer.getHexID(), 16);

        if(joiningVal < myVal) { // potentiall new lower neighbor
            if(lower == null || joiningVal > Long.parseLong(lower.getHexID(), 16)){
                lower = joiningPeer;
            }
        } else if(joiningVal > myVal) {
            if (higher == null || joiningVal < Long.parseLong(higher.getHexID(), 16)){
                higher = joiningPeer;
            }
        }
    }

    public PeerInfo findClosestNeighbor(String joiningNodeHexId) {

        if(lower == null && higher == null){ return null; }
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
        List<PeerInfo> leafset = new ArrayList<>();
        if(lower != null) { leafset.add(lower); }
        if(higher != null) { leafset.add(higher); }
        return leafset;
    }

    public int size() {
        int size = 0;
        if(lower != null) { size++; }
        if(higher != null) { size++; }
        return size;
    }

    public PeerInfo getlower() {
        return lower;
    }

    public PeerInfo getHigher() {
        return higher;
    }
}
