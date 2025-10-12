package csx55.pastry.util;

import csx55.pastry.wireformats.PeerInfo;

public class RoutingTable {
    
    private PeerInfo[][] rt = new PeerInfo[4][16];

    public void setPeerInfo(int row, int col, PeerInfo peerInfo){
        rt[row][col] = peerInfo;
    }

    public PeerInfo getPeerInfo(int row, int col) {
        return rt[row][col];
    }

    public PeerInfo checkRoutingTable(int row){
        for(int i = 0; i < rt.length; i++){
            if(rt[row][i] != null) {
                return rt[row][i];
            }
        }
        return null;
    }

    // print table method

}
