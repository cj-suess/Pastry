package csx55.pastry.wireformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        String myHexId = readString(dis);
        List<PeerInfo> leafsetList = readLeafset(dis);
        List<PeerInfo> rtList = readRoutingTable(dis);
        return new JoinResponse(messageType, respondingPeer, myHexId, leafsetList, rtList);
    }

    private List<PeerInfo> readLeafset(DataInputStream dis) throws IOException {
        List<PeerInfo> leafsetList = new ArrayList<>();
        int lsLength = dis.readInt();
        for(int i = 0; i < lsLength; i++) {
            leafsetList.add(readPeerInfo(dis));
        }
        return leafsetList;
    }

    private List<PeerInfo> readRoutingTable(DataInputStream dis) throws IOException {
        List<PeerInfo> rtList = new ArrayList<>();
        int rtLength = dis.readInt();
        for(int i = 0; i < rtLength; i++) {
            rtList.add(readPeerInfo(dis));
        }
        return rtList;
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
