package csx55.pastry.util;

import java.util.ArrayList;
import java.util.List;
import csx55.pastry.wireformats.*;
import java.util.logging.*;

public class Leafset {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(this.getClass().getName());

    private PeerInfo lower;
    private PeerInfo higher;
    private final long MAX_ID = 65536;

    public boolean remove(PeerInfo peer){
        if(peer != null && peer.equals(lower)) { 
            lower = null; 
            return true;
        }
        if(peer != null && peer.equals(higher)){ 
            higher = null; 
            return true;
        }
        return false;
    }

    public long clockwise(long from, long to) { // just flip for ccw for simplicity
        return (to - from + MAX_ID) % MAX_ID; 
    }

    public long calculateMinDistance(long x, long y) { // this will find the shortest numerical distance w/ wrap around
        long dist = Math.abs(x - y);
        return Math.min(dist, MAX_ID - dist);
    }

    public PeerInfo findClosestNeighbor(String joiningNodeHexId) {

        if(lower == null && higher == null){ return null; }
        if(lower == null) { return higher; } // if no smaller peer in ls return the higher peer
        if(higher == null || lower.equals(higher)) { return lower; } // if no higher peer in ls or lower and higher are the same then either only lower exists or an exact match was found

        long joiningVal = Long.parseLong(joiningNodeHexId, 16);
        long lowerVal = Long.parseLong(lower.getHexID(), 16);
        long higherVal = Long.parseLong(higher.getHexID(), 16);

        return (calculateMinDistance(joiningVal, lowerVal) <= calculateMinDistance(joiningVal, higherVal)) ? lower : higher;
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

    public PeerInfo getLower() {
        return lower;
    }

    public PeerInfo getHigher() {
        return higher;
    }

    public void setLower(PeerInfo newLower) {
        if(newLower != null){ this.lower = newLower; }
    }

    public void setHigher(PeerInfo newHigher) {
        if(newHigher != null) { this.higher = newHigher; }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(lower != null){
            sb.append("Lower: " + lower.toString() + "\n");
        }
        if(higher != null) {
            sb.append("Higher: " + higher.toString() + "\n");
        }
        return sb.toString();
    }
}
