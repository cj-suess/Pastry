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
                case Protocol.REGISTER_REQUEST:
                    readRegisterRequest(messageType, dis);
                case Protocol.REGISTER_RESPONSE:
                    readStatusMessage(messageType, dis);
                default:
                    break;
            }

        } catch(IOException e) {
            log.warning("Exception while creating event..." + e.getMessage());
        }
        return null;
    }

    private static Register readRegisterRequest(int messageType, DataInputStream dis) {
        try {
            String hexID = readString(dis);
            String ip = readString(dis);
            int port = dis.readInt();
            PeerInfo peerInfo = new PeerInfo(hexID, new ConnInfo(ip, port));
            Register register_request = new Register(messageType, peerInfo);
            return register_request;
        } catch(IOException e) {
            log.warning("Exception while decoding register request...." + e.getMessage());
        }
        log.warning("Returning null instead of Register_Request object...");
        return null;
    }

    private static String readString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes);
    }

    private static Message readStatusMessage(int messageType, DataInputStream dis) throws IOException {
        byte statusCode = dis.readByte();
        String info = readString(dis);
        return new Message(messageType, statusCode, info);
    }
    
}
