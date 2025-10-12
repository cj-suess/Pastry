package csx55.pastry.wireformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.*;

public class EventFactory {

    private final static Logger log = Logger.getLogger(EventFactory.class.getName());
    private final Consumer<Exception> warning = e -> log.log(Level.WARNING, e.getMessage(), e);
    private final byte[] data;

    public EventFactory(byte[] data) {
        this.data = data;
    }

    public Event createEvent() {
        try(ByteArrayInputStream bais = new ByteArrayInputStream(data); 
            DataInputStream dis = new DataInputStream(bais)) {

            int messageType = dis.readInt();

            switch (messageType) {
                case Protocol.REGISTER_REQUEST:
                case Protocol.DEREGISTER_REQUEST:
                    return readRegisterRequest(messageType, dis);
                case Protocol.REGISTER_RESPONSE:
                case Protocol.DEREGISTER_RESPONSE:
                    return readStatusMessage(messageType, dis);
                case Protocol.ENTRY_NODE:
                    return readEntryNode(messageType, dis);
                case Protocol.JOIN_REQUEST:
                    return readJoinRequest(messageType, dis);
                default:
                    warning.accept(null);
                    break;
            }

        } catch(IOException e) {
            warning.accept(e);
        }
        return null;
    }

    private JoinRequest readJoinRequest(int messageType, DataInputStream dis){
        try {
            String hexID = readString(dis);
            String ip = readString(dis);
            int port = dis.readInt();
            PeerInfo peerInfo = new PeerInfo(hexID, new ConnInfo(ip, port));
            return new JoinRequest(messageType, peerInfo);
        } catch(IOException e) {
            warning.accept(e);
        }
        warning.accept(null);
        return null;
    }

    private EntryNode readEntryNode(int messageType, DataInputStream dis) {
        try {
            String hexID = readString(dis);
            String ip = readString(dis);
            int port = dis.readInt();
            PeerInfo peerInfo = new PeerInfo(hexID, new ConnInfo(ip, port));
            return new EntryNode(messageType, peerInfo);
        } catch(IOException e) {
            warning.accept(e);
        }
        warning.accept(null);
        return null;
    }

    private Event readRegisterRequest(int messageType, DataInputStream dis) {
        try {
            String hexID = readString(dis);
            String ip = readString(dis);
            int port = dis.readInt();
            PeerInfo peerInfo = new PeerInfo(hexID, new ConnInfo(ip, port));
            Event request = messageType == Protocol.REGISTER_REQUEST ? new Register(messageType, peerInfo) : new Deregister(messageType, peerInfo);
            return request;
        } catch(IOException e) {
            warning.accept(e);
        }
        warning.accept(null);
        return null;
    }

    private String readString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes);
    }

    private Message readStatusMessage(int messageType, DataInputStream dis) throws IOException {
        String info = readString(dis);
        return new Message(messageType, info);
    }
    
}
