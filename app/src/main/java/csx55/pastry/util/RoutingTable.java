package csx55.pastry.util;

import java.util.ArrayList;
import java.util.List;

import csx55.pastry.wireformats.PeerInfo;

public class RoutingTable {
    
    private PeerInfo[][] rt = new PeerInfo[4][16];

    public void setPeerInfo(int row, int col, PeerInfo peerInfo){
        rt[row][col] = peerInfo;
    }

    public PeerInfo getPeerInfo(int row, int col) {
        return rt[row][col];
    }

    public List<PeerInfo> getAllPeers() {
        List<PeerInfo> peers = new ArrayList<>();
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 16; j++) {
                if(rt[i][j] != null) { peers.add(rt[i][j]); }
            }
        }
        return peers;
    }

    // print table method

}
