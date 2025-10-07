package csx55.pastry.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import csx55.pastry.wireformats.ConnInfo;
import java.util.logging.*;

public class OverlayCreator {

    private final static Logger log = Logger.getLogger(OverlayCreator.class.getName());

    private List<ConnInfo> nodes;
    private Map<ConnInfo, List<ConnInfo>> overlay = new ConcurrentHashMap<>();

    public OverlayCreator(){}

    public OverlayCreator(List<ConnInfo> nodes){
        this.nodes = nodes;
    }

    public Map<ConnInfo, List<ConnInfo>> buildRing() {
        log.info("Building overlay...");
        int n = nodes.size();
        for(int i = 0; i < n; i++) {
            overlay.put(nodes.get(i), new ArrayList<>());
            overlay.get(nodes.get(i)).add(nodes.get((i+1) % n));
        }
        return overlay;
    }
    
}
