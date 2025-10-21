package csx55.pastry.util;

import java.util.ArrayList;
import java.util.List;

import csx55.pastry.wireformats.PeerInfo;

public class RoutingTable {
    
    private String myHexId;
    private PeerInfo[][] rt = new PeerInfo[4][16];

    public RoutingTable(String myHexId) {
        this.myHexId = myHexId;
    }

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 4; i++) {
            String prefix = myHexId.substring(0,i);
            for(int j = 0; j < 16; j++) {
                if(rt[i][j] != null) {
                    sb.append(prefix + Integer.toHexString(j) + "-" + rt[i][j].getIP() + ":" + rt[i][j].getPort() + ",");
                } else {
                    sb.append(prefix + Integer.toHexString(j) + "-:,");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // public String printRoutingTable() {
    //     StringBuilder sb = new StringBuilder();
    //     for(int i = 0; i < 4; i++) {
    //         String prefix = myHexId.substring(0,i);
    //         for(int j = 0; j < 16; j++) {
    //             if(rt[i][j] != null) {
    //                 sb.append(prefix + Integer.toHexString(j) + "-" + rt[i][j].getIP() + ":" + rt[i][j].getPort() + ",");
    //             } else {
    //                 sb.append(prefix + Integer.toHexString(j) + "-:,");
    //             }
    //         }
    //         sb.append("\n");
    //     }
    //     return sb.toString();
    // }

}
