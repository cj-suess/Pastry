package csx55.pastry.wireformats;

import java.util.Objects;

public class PeerInfo implements Comparable<PeerInfo> {

    private final String hexID;
    private final ConnInfo conn;
    
    public PeerInfo(String hexID, ConnInfo conn){
        this.hexID = hexID;
        this.conn = conn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hexID);
    }

    @Override
    public int compareTo(PeerInfo o) {
        return this.hexID.compareTo(o.hexID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof PeerInfo)) return false;
        PeerInfo peerInfo = (PeerInfo) o;
        return Objects.equals(hexID, peerInfo.hexID);
    }

    @Override
    public String toString() {
        return conn.getIP() + ":" + conn.getPort() + ", " + getHexID();
    }

    public String getHexID(){
        return hexID;
    }

    public String getIP() {
        return conn.getIP();
    }

    public int getPort() {
        return conn.getPort();
    }
}
