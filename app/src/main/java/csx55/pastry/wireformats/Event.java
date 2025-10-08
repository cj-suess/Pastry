package csx55.pastry.wireformats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Event {

    public abstract int getType();
    abstract void marshalData(DataOutputStream dout) throws IOException;

    public byte[] getBytes() throws IOException {
        byte[] encodedData = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(baos);
        dout.writeInt(this.getType());
        marshalData(dout);
        dout.flush();
        encodedData = baos.toByteArray();
        baos.close();
        dout.close();
        return encodedData;
    }

    void writeString(DataOutputStream dout, String ip) throws IOException {
        byte[] ipBytes = ip.getBytes();
        int ipLength = ipBytes.length;
        dout.writeInt(ipLength);
        dout.write(ipBytes);
    }
}
