package csx55.pastry.wireformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.*;

public class EventFactory {

    private final static Logger log = Logger.getLogger(EventFactory.class.getName());
    private final byte[] data;

    public EventFactory(byte[] data) {
        this.data = data;
    }

    public Event createEvent() {
        try(ByteArrayInputStream bais = new ByteArrayInputStream(data); DataInputStream dis = new DataInputStream(bais)) {

            int messageType = dis.readInt();

            switch (messageType) {
                default:
                    break;
            }

        } catch(IOException e) {
            log.warning("Exception while creating event..." + e.getMessage());
        }
        return null;
    }
    
}
