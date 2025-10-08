package csx55.pastry.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Register implements Event, Protocol {

    public int messageType;
    public PeerInfo peerInfo;

    public Register(int messageType, PeerInfo peerInfo) {
        this.messageType = messageType;
        this.peerInfo = peerInfo;
    }

    @Override
    public byte[] getBytes() throws IOException{
        byte[] encodedData = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(baos);
        dout.writeInt(messageType);

        /* FILL IN REQURED MARSHALING */
        byte[] ipBytes = peerInfo.getIP().getBytes();
        int ipLength = ipBytes.length;
        dout.writeInt(ipLength);
        dout.write(ipBytes);
        dout.writeInt(peerInfo.getPort());
        /*              
         * 
                     */
        dout.flush();
        encodedData = baos.toByteArray();
        baos.close();
        dout.close();
        return encodedData;
    }

    @Override
    public int getType() {
        return Protocol.REGISTER_REQUEST;
    }
}
