package csx55.pastry.node;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.util.*;
import csx55.pastry.wireformats.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.*;

public class Peer implements Node {

    private Logger log = Logger.getLogger(this.getClass().getName());
    private final Consumer<Exception> warning = e -> log.log(Level.WARNING, e.getMessage(), e);

    private boolean running = true;
    private ConnInfo regNodeInfo;
    private TCPConnection regConn;

    private String myHexID;
    public PeerInfo myPeerInfo;
    Leafset ls = new Leafset();
    RoutingTable rt = new RoutingTable();

    private Map<Socket, TCPConnection> socketToConn = new ConcurrentHashMap<>();

    private Map<Integer, Consumer<Event>> events = new HashMap<>();
    private Map<String, Runnable> commands = new HashMap<>();

    public Peer(String host, int port, String hexID) {
        regNodeInfo = new ConnInfo(host, port);
        this.myHexID = hexID;
        startsEvents();
        startCommands();
    }

    public Peer(String host, int port){
        regNodeInfo = new ConnInfo(host, port);
        this.myHexID = String.format("%04X", ThreadLocalRandom.current().nextInt(65536));
        startsEvents();
        startCommands();
    }


    @Override
    public void onEvent(Event event, Socket socket) {
        if(event != null) {
            Consumer<Event> handler = events.get(event.getType());
            handler.accept(event);
        } else {
            warning.accept(null);
        }
    }

    private void startsEvents() {
        events = Map.of(
            Protocol.REGISTER_RESPONSE, this::registerResponse,
            Protocol.DEREGISTER_RESPONSE, this::deregisterResponse,
            Protocol.ENTRY_NODE, this::processEntryNode,
            Protocol.JOIN_REQUEST, this::processJoinRequest,
            Protocol.JOIN_RESPONSE, this::processJoinResponse
        );
    }

    private void processJoinResponse(Event event) {
        JoinResponse joinResponse = (JoinResponse) event;
        log.info(() -> "Received join response from --> " + joinResponse.getPeerInfo().toString());
        
    }

    private void processJoinRequest(Event event) {
        JoinRequest joinRequest = (JoinRequest) event;
        log.info(() -> "Received join request for --> " + joinRequest.peerInfo.toString());
        String joiningHexId = joinRequest.peerInfo.getHexID();
        PeerInfo joiningPeerInfo = joinRequest.peerInfo;

        PeerInfo closestPeer = ls.findClosestNeighbor(joiningHexId);
        if(closestPeer != null && isCloser(joiningHexId, closestPeer.getHexID(), myHexID)) { // I havc a closer peer in my ls and it is closer than the joining peer
            log.info(() -> "Forwarding request to peer in leafset --> " + closestPeer.getHexID());
            forwardRequest(joinRequest, closestPeer);
            return;
        }
        int lmpl = longestMatchingPrefixLength(myHexID, joiningHexId);
        if(lmpl < 4) { // we are not the destination
            if(rt.getPeerInfo(lmpl, lmpl+1) != null) { // we can make a jump to peer in rt
                PeerInfo rtPeer = rt.getPeerInfo(lmpl, lmpl+1);
                log.info(() -> "Forwarding request to peer in routing table --> " + rtPeer.getHexID());
                forwardRequest(joinRequest, rtPeer);
                return;
            }
        }
        PeerInfo closestOverallPeer = closestOverallPeer(joiningHexId);
        if(closestOverallPeer != null && isCloser(joiningHexId, closestOverallPeer.getHexID(), myHexID)) { // if the closest overall peer is closer than the joining peer
            log.info(() -> "Forwarding request to closest overall peer --> " + closestOverallPeer.getHexID());
            forwardRequest(joinRequest, closestOverallPeer);
            return;
        }
        // I am the closest peer to the joining peer
        log.info(() -> "Sending join response back to --> " + joinRequest.peerInfo.getHexID());
        sendJoinResponse(joiningPeerInfo, ls, rt);
    }

    private void sendJoinResponse(PeerInfo joinPeerInfo, Leafset ls, RoutingTable rt) {
        JoinResponse joinResponse = new JoinResponse(Protocol.JOIN_REQUEST, myPeerInfo, ls, rt);
        try (Socket socket = new Socket(joinPeerInfo.getIP(), joinPeerInfo.getPort());) {
            TCPConnection conn = new TCPConnection(socket, this);
            socketToConn.put(socket, conn);
            conn.startReceiverThread();
            conn.sender.sendData(joinResponse.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private void forwardRequest(JoinRequest joinRequest, PeerInfo peerInfo) {
        try (Socket socket = new Socket(peerInfo.getIP(), peerInfo.getPort());) {
            TCPConnection conn = new TCPConnection(socket, this);
            socketToConn.put(socket, conn);
            conn.startReceiverThread();
            conn.sender.sendData(joinRequest.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private PeerInfo closestOverallPeer(String joiningNodeHexId) {
        PeerInfo closestOverallPeer = null;
        long minDistance = -Long.MAX_VALUE;
        long joiningNodeVal = Long.parseLong(joiningNodeHexId, 16);
        List<PeerInfo> allPeers = rt.getAllPeers();
        allPeers.addAll(ls.getAllPeers());
        for(PeerInfo peer : allPeers) {
            long currPeerVal = Long.parseLong(peer.getHexID(), 16);
            long distance = Math.abs(joiningNodeVal - currPeerVal);
            if(distance < minDistance){
                minDistance = distance;
                closestOverallPeer = peer;
            }
        }
        return closestOverallPeer;
    }

    private int longestMatchingPrefixLength(String id1, String id2) {
        for(int i = 0; i < id1.length(); i++) {
            if(id1.charAt(i) != id2.charAt(i)){ return i; }
        }
        return id1.length();
    }

    private boolean isCloser(String joiningHexId, String closestPeerHexId, String myHexId) {
        return Math.abs(Long.parseLong(joiningHexId, 16) - Long.parseLong(closestPeerHexId, 16)) < Math.abs(Long.parseLong(joiningHexId, 16) - Long.parseLong(myHexId, 16));
    }

    private void processEntryNode(Event event){
        EntryNode entryNode = (EntryNode) event;
        log.info(() -> "Received entry node from Discovery: " + entryNode.peerInfo.toString());
        log.info(() -> "Sending join request...");
        sendJoinRequest(entryNode.peerInfo.getIP(), entryNode.peerInfo.getPort());
    }

    private void sendJoinRequest(String host, int port) {
        JoinRequest joinRequest = new JoinRequest(Protocol.JOIN_REQUEST, myPeerInfo);
        try (Socket socket = new Socket(host, port);) {
            TCPConnection conn = new TCPConnection(socket, this);
            socketToConn.put(socket, conn);
            conn.startReceiverThread();
            conn.sender.sendData(joinRequest.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private void registerResponse(Event event) {
        log.info(() -> "Received register response from Discovery...");
        Message responseEvent = (Message) event; 
        log.info(() -> responseEvent.info);
    }

    private void deregisterResponse(Event event) {
        log.info(() -> "Received deregister response from Discovery...");
        Message responseEvent = (Message) event;
        log.info(() -> responseEvent.info);
    }

    private void register() {
        try{
            Socket socket = new Socket(regNodeInfo.getIP(), regNodeInfo.getPort());
            regConn = new TCPConnection(socket, this);
            Register registerMessage = new Register(Protocol.REGISTER_REQUEST, myPeerInfo);
            regConn.startReceiverThread();
            regConn.sender.sendData(registerMessage.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private void deregister() {
        log.info(() -> "Deregistering node...");
            Deregister deregisterRequest = new Deregister(Protocol.DEREGISTER_REQUEST, myPeerInfo);
            try {
                regConn.sender.sendData(deregisterRequest.getBytes());
            } catch (IOException e) {
                warning.accept(e);
            }
    }

    private void startNode() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            myPeerInfo = new PeerInfo(myHexID, new ConnInfo(InetAddress.getLocalHost().getHostAddress(), serverSocket.getLocalPort()));
            log = Logger.getLogger(Peer.class.getName() + "[" + myPeerInfo.toString() + "]");
            register();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    deregister();
                    serverSocket.close();
                } catch(IOException e) {
                    warning.accept(e);
                }
            }));
            while(running) {
                Socket clientSocket = serverSocket.accept();
                log.info(() -> "New connection from: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                TCPConnection conn = new TCPConnection(clientSocket, this);
                conn.startReceiverThread();
                socketToConn.put(clientSocket, conn);
            }
        }catch(IOException e) {
            warning.accept(e);
        }
    }

    private void readTerminal() {
        try(Scanner scanner = new Scanner(System.in)) {
            while(true) {
                String command = scanner.nextLine();
                commands.get(command).run();
            }
        } 
    }

    private void startCommands() {
        commands.put("exit", this::deregister);
    }

    public static void main(String[] args) {

        LogConfig.init(Level.INFO);
        Peer peer;
        peer = (args.length > 2) ? new Peer(args[0], Integer.parseInt(args[1]), args[2]) : new Peer(args[0], Integer.parseInt(args[1]));
        new Thread(peer::startNode, "Node-" + peer.toString() + "-Server").start();
        new Thread(peer::readTerminal, "Node-" + peer.toString() + "-Terminal").start();

    }
}
