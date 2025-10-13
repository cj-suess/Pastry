package csx55.pastry.wireformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.*;

import csx55.pastry.util.Leafset;
import csx55.pastry.util.RoutingTable;

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
                case Protocol.JOIN_RESPONSE:
                    return readJoinResponse(messageType, dis);
                default:
                    warning.accept(null);
                    break;
            }

        } catch(IOException e) {
            warning.accept(e);
        }
        return null;
    }

    private JoinResponse readJoinResponse(int messageType, DataInputStream dis) throws IOException {
        PeerInfo respondingPeer = readPeerInfo(dis);
        Leafset ls = readLeafset(dis);
        RoutingTable rt = readRoutingTable(dis);
        return new JoinResponse(messageType, respondingPeer, ls, rt);
    }

    private Leafset readLeafset(DataInputStream dis) throws IOException {
        Leafset ls = new Leafset();
        int lsLength = dis.readInt();
        for(int i = 0; i < lsLength; i++) {
            PeerInfo peer = readPeerInfo(dis);
            ls.addPeer(peer);
        }
        return ls;
    }

    private RoutingTable readRoutingTable(DataInputStream dis) throws IOException {
        RoutingTable rt = new RoutingTable();
        int rtLength = dis.readInt();
        for(int k = 0; k < rtLength; k++) {
            int i = dis.readInt();
            int j = dis.readInt();
            PeerInfo peer = readPeerInfo(dis);
            rt.setPeerInfo(i, j, peer);
        }
        return rt;
    }

    private JoinRequest readJoinRequest(int messageType, DataInputStream dis) throws IOException {
        PeerInfo peerInfo = readPeerInfo(dis);
        return new JoinRequest(messageType, peerInfo);
    }

    private EntryNode readEntryNode(int messageType, DataInputStream dis) throws IOException {
        PeerInfo peerInfo = readPeerInfo(dis);
        return new EntryNode(messageType, peerInfo);
    }

    private Event readRegisterRequest(int messageType, DataInputStream dis) throws IOException {
        PeerInfo peerInfo = readPeerInfo(dis);
        Event request = messageType == Protocol.REGISTER_REQUEST ? new Register(messageType, peerInfo) : new Deregister(messageType, peerInfo);
        return request;
    }

    private PeerInfo readPeerInfo(DataInputStream dis) throws IOException {
        String hexID = readString(dis);
        String ip = readString(dis);
        int port = dis.readInt();
        return new PeerInfo(hexID, new ConnInfo(ip, port));
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
