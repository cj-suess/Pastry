package csx55.pastry.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Overlay extends Event {
    
    public int messageType;
    public int numNodes;
    public int numConnections;
    public Map<ConnInfo, List<ConnInfo>> overlay;

    public Overlay(int messageType, int numNodes, int numConnections, Map<ConnInfo, List<ConnInfo>> overlay) {
        this.messageType = messageType;
        this.numNodes = numNodes;
        this.numConnections = numConnections;
        this.overlay = overlay;
    }

    @Override
    public int getType() {
        return Protocol.OVERLAY;
    }

    @Override
    public byte[] getBytes() throws IOException {
        byte[] encodedData = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(baos);
        dout.writeInt(messageType);
        /* FILL IN REQURED MARSHALING */
        dout.writeInt(numNodes);
        dout.writeInt(numConnections);
        writeMappings(dout, overlay);
        /*                           */
        dout.flush();
        encodedData = baos.toByteArray();
        baos.close();
        dout.close();
        return encodedData;
    }

    // method to write each String -> List mapping
    private void writeMappings(DataOutputStream dout, Map<ConnInfo, List<ConnInfo>> overlay) throws IOException {
        for(Map.Entry<ConnInfo, List<ConnInfo>> entry : overlay.entrySet()) {
            byte[] ipBytes = entry.getKey().getIP().getBytes();
            int ipLength = ipBytes.length;
            dout.writeInt(ipLength);
            dout.write(ipBytes);
            dout.writeInt(entry.getKey().getPort());
            for(ConnInfo node : entry.getValue()) {
                ipBytes = node.getIP().getBytes();
                ipLength = ipBytes.length;
                dout.writeInt(ipLength);
                dout.write(ipBytes);
                dout.writeInt(node.getPort());
            }
        }
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'marshalData'");
    }
}

