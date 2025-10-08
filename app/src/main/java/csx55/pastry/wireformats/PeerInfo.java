package csx55.pastry.wireformats;

import java.util.logging.*;

public class PeerInfo {
    
    private Logger log = Logger.getLogger(this.getClass().getName());

    public String hexID;
    public ConnInfo conn;
    public String nickname;
    
    public PeerInfo(String hexID, ConnInfo conn, String nickname){
        this.hexID = hexID;
        this.conn = conn;
        this.nickname = nickname;
    }

    public void setHexID(String hexID) {
        this.hexID = hexID;
    }

    public String getIP() {
        return conn.getIP();
    }

    public int getPort() {
        return conn.getPort();
    }
}
