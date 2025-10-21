package csx55.pastry.util;

import java.util.ArrayList;
import java.util.List;
import csx55.pastry.wireformats.*;
import java.util.logging.*;

public class Leafset {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private PeerInfo lower;
    private PeerInfo higher;
    private long MAX_ID = 65536;

    private long calculateDistance(long from, long to) { // calculate the clockwise distance between two peers
        if(to >= from) {
            return to - from;
        } else { // wrap around
            return MAX_ID - from + to;
        }
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
        long distanceToJoining = calculateDistance(myVal, joiningVal);

        if(distanceToJoining <= MAX_ID / 2) { // potentiall new higher neighbor
            if(higher == null) {
                log.info(() -> "Setting higher initially to --> " + joiningPeer.getHexID());
                higher = joiningPeer;
                changed = true;
            } else {
                long distanceToCurrentHigher = calculateDistance(myVal, Long.parseLong(higher.getHexID(), 16));
                if(distanceToJoining < distanceToCurrentHigher) {
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
                long distanceToCurrentLower = calculateDistance(Long.parseLong(lower.getHexID(), 16), myVal); // counter clockwise check
                long distanceToJoiningFromMyVal = calculateDistance(joiningVal, myVal);
                if(distanceToJoiningFromMyVal < distanceToCurrentLower) {
                    log.info(() -> "Updating lower neighbor to --> " + joiningPeer.getHexID());
                    lower = joiningPeer;
                    changed = true;
                }
            }
        }
        return changed;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(lower != null){
            sb.append(lower.toString() + "\n");
        }
        if(higher != null) {
            sb.append(higher.toString() + "\n");
        }
        return sb.toString();
    }
}
