package csx55.pastry.node;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.util.Converter;
import csx55.pastry.util.LogConfig;
import csx55.pastry.wireformats.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final Object lock = new Object();
    private final Converter c = Converter.getConverter();

    private boolean running = true;
    private final ConnInfo discConnInfo;
    private final String mode;
    private final String filePath;

    private final Map<Socket, TCPConnection> socketToConn = new ConcurrentHashMap<>();
    private final Map<String, TCPConnection> peerToConn = new ConcurrentHashMap<>();

    private Map<Integer, BiConsumer<Event, Socket>> events = new HashMap<>();

    public Data(String discHost, int discPort,  String mode, String filePath) {
        discConnInfo = new ConnInfo(discHost, discPort);
        this.mode = mode;
        this.filePath = filePath;
        startEvents();
    }

    private void startEvents() {
        events = Map.of(
            Protocol.ENTRY_NODE, this::processEntryNode
        );
    }

    private void processEntryNode(Event event, Socket socket) {
        EntryNode entryNodeMessage = (EntryNode) event;
        log.info(() -> "Received entry node from Discovery --> " + entryNodeMessage.peerInfo.toString());
        PeerInfo entryNode = entryNodeMessage.peerInfo;
        if(mode.equals("store")) {
            store();
        } else if(mode.equals("retrieve")) {
            retrieve();
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
            log = Logger.getLogger(Data.class.getName());
            getEntryNode();
            while(running) {
                Socket socket = serverSocket.accept();
                TCPConnection conn = new TCPConnection(socket, this);
                conn.startReceiverThread();
                socketToConn.put(socket, conn);
            }
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private void getEntryNode() {
        try{
            Socket socket = new Socket(discConnInfo.getIP(), discConnInfo.getPort());
            TCPConnection conn = new TCPConnection(socket, this);
            EntryNode getEntryNode = new EntryNode(Protocol.ENTRY_NODE, null);
            conn.startReceiverThread();
            conn.sender.sendData(getEntryNode.getBytes());
        } catch (IOException e) {
            warning.accept(e);
        }
    }

    private void store() {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        String fileHex = c.convertBytesToHex(Converter.hash16(fileName));
        // StoreRequest storeRequest = new StoreRequest(Protocol.STORE_REQUEST, fileHex);

    }

    private void retrieve() {

    }

    public static void main(String[] args) {

        LogConfig.init(Level.INFO);
        Data data = new Data(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        new Thread(data::startNode, "Node-" + data.toString() + "-Server").start();

    }
}
