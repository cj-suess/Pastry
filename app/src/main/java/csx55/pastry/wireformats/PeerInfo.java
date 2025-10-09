package csx55.pastry.wireformats;

import java.util.logging.*;
import java.util.Objects;

public class PeerInfo implements Comparable<PeerInfo> {
    
    private Logger log = Logger.getLogger(this.getClass().getName());

    public String hexID;
    public ConnInfo conn;
    
    public PeerInfo(String hexID, ConnInfo conn){
        this.hexID = hexID;
        this.conn = conn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hexID, conn);
    }

    @Override
    public int compareTo(PeerInfo o) {
        return this.hexID.compareTo(o.hexID);
    }

    public void setHexID(String hexID) {
        this.hexID = hexID;
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
