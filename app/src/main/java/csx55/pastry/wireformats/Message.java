package csx55.pastry.wireformats;

import java.io.DataOutputStream;
import java.io.IOException;

public class Message extends Event {
    
    public int messageType;
    public String info;

    public Message(int messageType, String info) {
        this.messageType = messageType;
        this.info = info;
    }

    @Override
    public int getType() {
        return messageType;
    }

    @Override
    void marshalData(DataOutputStream dout) throws IOException {
        writeString(dout, info);
    }
    
}
