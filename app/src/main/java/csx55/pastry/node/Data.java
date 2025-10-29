package csx55.pastry.node;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.util.Converter;
import csx55.pastry.util.LogConfig;
import csx55.pastry.wireformats.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Data implements Node {

    private Logger log = Logger.getLogger(this.getClass().getName());
    private final Consumer<Exception> warning = e -> log.log(Level.WARNING, e.getMessage(), e);

    private final Converter c = Converter.getConverter();

    private final ConnInfo discConnInfo;
    private final String mode;
    private final String filePath;
    private PeerInfo myPeerInfo;

    private final Map<Socket, TCPConnection> socketToConn = new ConcurrentHashMap<>();

    private Map<Integer, BiConsumer<Event, Socket>> events = new HashMap<>();

    public Data(String discHost, int discPort,  String mode, String filePath) {
        discConnInfo = new ConnInfo(discHost, discPort);
        this.mode = mode;
        this.filePath = filePath;
        this.myPeerInfo = null;
        startEvents();
    }

    private void startEvents() {
        events = Map.of(
            Protocol.ENTRY_NODE, this::processEntryNode,
            Protocol.STORE_RESPONSE, this::processStoreResponse,
            Protocol.RETRIEVE_RESPONSE, this::processRetrieveResponse
        );
    }

    private void processRetrieveResponse(Event event, Socket socket) {
        log.info(() -> "Received retrieve response....");
        RetrieveResponse retrieveResponse = (RetrieveResponse) event;
        for(String s : retrieveResponse.getRoutingPath()) {
            System.out.println(s);
        }
        System.out.println(c.convertBytesToHex(Converter.hash16(Paths.get(filePath).getFileName().toString())));
        System.exit(0);
    }

    private void processStoreResponse(Event event, Socket socket) {
        log.info(() -> "Received store response....");
        StoreResponse storeResponse = (StoreResponse) event;
        for(String s : storeResponse.getRoutingPath()) {
            System.out.println(s);
        }
        System.out.println(c.convertBytesToHex(Converter.hash16(Paths.get(filePath).getFileName().toString())));
        System.exit(0);
    }

    private void processEntryNode(Event event, Socket socket) {
        EntryNode entryNodeMessage = (EntryNode) event;
        log.info(() -> "Received entry node from Discovery --> " + entryNodeMessage.peerInfo.toString());
        PeerInfo entryNode = entryNodeMessage.peerInfo;
        if(mode.equals("store")) {
            store(entryNode);
        } else if(mode.equals("retrieve")) {
            retrieve(entryNode);
        }
    }

    @Override
    public void onEvent(Event event, Socket socket) {
        if(event != null) {
            BiConsumer<Event, Socket> handler = events.get(event.getType());
            if(handler != null) {
                handler.accept(event, socket);
            }
        }
    }

    @Override
    public void startNode() {
        try(ServerSocket serverSocket = new ServerSocket(0)){
            myPeerInfo = new PeerInfo("", new ConnInfo(InetAddress.getLocalHost().getHostAddress(), serverSocket.getLocalPort()));
            log = Logger.getLogger(Data.class.getName());
            getEntryNode();
            while(true) {
                Socket socket = serverSocket.accept();
                TCPConnection conn = new TCPConnection(socket, this);
                socketToConn.put(socket, conn);
                conn.startReceiverThread();
            }
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private void getEntryNode() {
        try{
            Socket socket = new Socket(discConnInfo.getIP(), discConnInfo.getPort());
            TCPConnection conn = new TCPConnection(socket, this);
            EntryNode getEntryNode = new EntryNode(Protocol.ENTRY_NODE, new PeerInfo("", new ConnInfo("", 0)));
            conn.startReceiverThread();
            conn.sender.sendData(getEntryNode.getBytes());
        } catch (IOException e) {
            warning.accept(e);
        }
    }

    private void store(PeerInfo peerInfo) {
        try {
            Path path = Paths.get(filePath);
            String fileName = path.getFileName().toString();
            byte[] data = Files.readAllBytes(path);
            StoreRequest storeRequest = new StoreRequest(Protocol.STORE_REQUEST, myPeerInfo, fileName, data, new ArrayList<>());
            sendRequest(storeRequest, peerInfo);
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private void sendRequest(Event event,  PeerInfo peerInfo) {
        try {
            Socket socket = new Socket(peerInfo.getIP(), peerInfo.getPort());
            TCPConnection conn = new TCPConnection(socket, this);
            socketToConn.put(socket, conn);
            conn.startReceiverThread();
            conn.sender.sendData(event.getBytes());
        } catch (IOException e){
            warning.accept(e);
        }
    }

    private void retrieve(PeerInfo peerInfo) {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        RetrieveRequest retrieveRequest = new RetrieveRequest(Protocol.RETRIEVE_REQUEST, myPeerInfo, fileName, new ArrayList<>());
        sendRequest(retrieveRequest, peerInfo);
    }

    public static void main(String[] args) {

        LogConfig.init(Level.INFO);
        Data data = new Data(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        new Thread(data::startNode, "Node-" + data.toString() + "-Server").start();

    }
}
