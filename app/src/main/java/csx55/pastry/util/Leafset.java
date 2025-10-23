package csx55.pastry.util;

import java.util.ArrayList;
import java.util.List;
import csx55.pastry.wireformats.*;
import java.util.logging.*;

public class Leafset {

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

    private long clockwise(long x, long y) { // calculate the clockwise distance between two peers
        return (y - x + MAX_ID) % MAX_ID; 
    }

    private long counterClockwise(long x, long y) {
        return (x - y + MAX_ID) % MAX_ID;
    }
 
    public boolean addPeer(PeerInfo joiningPeer, String myHexId) {
        boolean changed = false;
        if(joiningPeer == null) {
            log.info("Could not add peer. Joining peer was null...");
            return changed;
        }
        if(joiningPeer.getHexID().equals(myHexId)) {
            log.info("Could not add peer. Joining peer hexID was my own...");
            return changed;
        }

        long myVal = Long.parseLong(myHexId, 16);
        long joiningVal = Long.parseLong(joiningPeer.getHexID(), 16);
        long cw = clockwise(myVal, joiningVal);
        long ccw = counterClockwise(myVal, joiningVal);

        if(cw <= ccw) { // potentiall new higher neighbor
            if(higher == null) {
                log.info(() -> "Setting higher initially to --> " + joiningPeer.getHexID());
                higher = joiningPeer;
                changed = true;
            } else {
                long currentHigherCW = clockwise(myVal, Long.parseLong(higher.getHexID(), 16));
                if(cw < currentHigherCW) {
                    log.info(() -> "Updating higher neighbor to --> " + joiningPeer.getHexID());
                    higher = joiningPeer;
                    changed = true;
                }
            }
        } else { // check for new lower neighbor
            if(lower == null) {
                log.info(() -> "Setting lower initially to --> " + joiningPeer.getHexID());
                lower = joiningPeer;
                changed = true;
            } else {
                long currLowerCCW = counterClockwise(myVal, Long.parseLong(lower.getHexID(), 16));
                if(ccw < currLowerCCW) {
                    log.info(() -> "Updating lower neighbor to --> " + joiningPeer.getHexID());
                    lower = joiningPeer;
                    changed = true;
                }
            }
        }
        return changed;
    }

    public long calculateMinDistance(long x, long y) {
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

        if(calculateMinDistance(joiningVal, lowerVal) <= calculateMinDistance(joiningVal, higherVal)) {
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

    public PeerInfo getLower() {
        return lower;
    }

    public PeerInfo getHigher() {
        return higher;
    }

    public void setLower(PeerInfo newLower) {
        this.lower = newLower;
    }

    public void setHigher(PeerInfo newHigher) {
        this.higher = newHigher;
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
