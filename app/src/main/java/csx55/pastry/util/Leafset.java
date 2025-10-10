package csx55.pastry.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import csx55.pastry.wireformats.PeerInfo;

public class Leafset {
    
    List<PeerInfo> neighbors = Collections.synchronizedList(new ArrayList<>());

    public Leafset() {

    }

    public List<PeerInfo> getLeafSet(){
        return neighbors;
    }
    
}
