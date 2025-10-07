package csx55.pastry.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Deregister implements Event {

    public int messageType;
    public ConnInfo connInfo;

    public Deregister(int messageType, ConnInfo connInfo) {
        this.messageType = messageType;
        this.connInfo = connInfo;
    }

    @Override
    public byte[] getBytes() throws IOException{
        byte[] encodedData = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(baos);
        dout.writeInt(messageType);

        /* FILL IN REQURED MARSHALING */
        byte[] ipBytes = connInfo.getIP().getBytes();
        int ipLength = ipBytes.length;
        dout.writeInt(ipLength);
        dout.write(ipBytes);
        dout.writeInt(connInfo.getPort());
        /*                             */
        
        dout.flush();
        encodedData = baos.toByteArray();
        
        baos.close();
        dout.close();
        return encodedData;
    }

    @Override
    public int getType() {
        return Protocol.DEREGISTER_REQUEST;
    }
    
}