package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;

public class Message extends Event {
    
    public int messageType;
    public byte statusCode;
    public String info;

    public Message(int messageType, byte statusCode, String info) {
        this.messageType = messageType;
        this.statusCode = statusCode;
        this.info = info;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        dout.writeByte(statusCode);
        writeString(dout, info);
    }
    
}
