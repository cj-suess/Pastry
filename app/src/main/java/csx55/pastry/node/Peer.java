package csx55.pastry.node;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.util.*;
import csx55.pastry.wireformats.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
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
    Leafset ls;
    RoutingTable rt;

    private Map<Socket, TCPConnection> socketToConn = new ConcurrentHashMap<>();
    private Map<PeerInfo, TCPConnection> peerToConn = new ConcurrentHashMap<>();

    private Map<Integer, BiConsumer<Event, Socket>> events = new HashMap<>();
    private Map<String, Runnable> commands = new HashMap<>();

    public Peer(String host, int port, String hexID) {
        regNodeInfo = new ConnInfo(host, port);
        this.myHexID = hexID;
        ls = new Leafset();
        rt = new RoutingTable(myHexID);
        startsEvents();
        startCommands();
    }

    public Peer(String host, int port){
        regNodeInfo = new ConnInfo(host, port);
        this.myHexID = String.format("%04X", ThreadLocalRandom.current().nextInt(65536));
        ls = new Leafset();
        rt = new RoutingTable(myHexID);
        startsEvents();
        startCommands();
    }


    @Override
    public void onEvent(Event event, Socket socket) {
        if(event != null) {
            BiConsumer<Event, Socket> handler = events.get(event.getType());
            handler.accept(event, socket);
        }
    }

    private void startsEvents() {
        events = Map.of(
            Protocol.REGISTER_RESPONSE, this::registerResponse,
            Protocol.DEREGISTER_RESPONSE, this::deregisterResponse,
            Protocol.ENTRY_NODE, this::processEntryNode,
            Protocol.JOIN_REQUEST, this::processJoinRequest,
            Protocol.JOIN_RESPONSE, this::processJoinResponse, 
            Protocol.UPDATE, this::processUpdate,
            Protocol.EXIT, this::processExit
        );
    }

    private void processExit(Event event, Socket socket) {
        Exit exitMessage = (Exit) event;
        PeerInfo exitingPeer = exitMessage.getExitingPeer();
        PeerInfo newNeighbor =  exitMessage.getNewNeighbor();
        ls.remove(exitingPeer);
        rt.remove(exitingPeer);
        boolean changed = false;
        if(newNeighbor != null){
            changed = updateTables(newNeighbor);
        }
        if(changed) {
            log.info(() -> "I have changes. Updating peers...");
            updateAllPeers();
        }
    }

    private void processUpdate(Event event, Socket socket) {
        Update update = (Update) event;
        PeerInfo sender = update.getMyPeerInfo();
        log.info(() -> "Received update message from --> " + sender.toString());
        boolean changed = updateTables(sender); // check if a change occurred

        for(PeerInfo p : update.getPeers()) {
            updateTables(p);
        }

        if(changed) { // update peers in leafset if something changed in my leafset
            log.info(() -> "My leafset changed. Updating peers in my leafset...");
            updateLeafset(sender);
        }
    }

    private void updateLeafset(PeerInfo sender){
        List<PeerInfo> peers = ls.getAllPeers();
        for(PeerInfo p : peers) {
            if(!p.equals(sender)) {
                log.info(() -> "Updating --> " + p.getHexID());
                sendUpdateMessage(p, peers);
            }
        }
    }

    private void processJoinResponse(Event event, Socket socket) {
        JoinResponse joinResponse = (JoinResponse) event;
        PeerInfo joiningPeerInfo = joinResponse.getPeerInfo();
        log.info(() -> "Received join response from --> " + joiningPeerInfo.toString());
        
        updateTables(joiningPeerInfo); // update LS with joining peer

        for(PeerInfo p : joinResponse.getLeafset()) { // make updates with joining peer's LS
            updateTables(p);
        }

        for(PeerInfo p : joinResponse.getRoutingTable()) { // make updates with joining peer's RT
            updateTables(p);
        }

        updateAllPeers(); // update all nodes in LS and RT
    }

    private void updateAllPeers() {
        List<PeerInfo> peers = rt.getAllPeers();
        peers.addAll(ls.getAllPeers());
        for(PeerInfo p : peers) {
            log.info(() -> "Sending update message to --> " + p.getHexID());
            sendUpdateMessage(p, peers);
        }
    }

    private void sendUpdateMessage(PeerInfo p, List<PeerInfo> peers) {
        Update update = new Update(Protocol.UPDATE, myPeerInfo, peers);
        try {
            TCPConnection conn = peerToConn.get(p);
            if(conn == null){
                Socket socket = new Socket(p.getIP(), p.getPort());
                conn = new TCPConnection(socket, this);
                conn.startReceiverThread();
                socketToConn.put(socket, conn);
                peerToConn.put(p, conn);
            }
            conn.sender.sendData(update.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private boolean updateTables(PeerInfo joiningPeerInfo) {
        boolean changed = false;
        if(joiningPeerInfo.getHexID().equals(myHexID)) { // dont' add myself
            return changed;
        }
        if(ls.addPeer(joiningPeerInfo, myHexID)) { // add to ls (will only add if it is able to update one of my current neighbors)
            changed = true;
        }
        // now update routing table with any peer that I can use
        int row = longestMatchingPrefixLength(myHexID, joiningPeerInfo.getHexID());
        if(row < 4) { // don't add myself
            int col = Character.digit(joiningPeerInfo.getHexID().charAt(row), 16);
            if(rt.getPeerInfo(row, col) == null) {
                rt.setPeerInfo(row, col, joiningPeerInfo);
            }
        }
        return changed;
    }

    private void processJoinRequest(Event event, Socket socket) {
        JoinRequest joinRequest = (JoinRequest) event;
        log.info(() -> "Received join request for --> " + joinRequest.peerInfo.toString());
        String joiningHexId = joinRequest.peerInfo.getHexID();
        PeerInfo joiningPeerInfo = joinRequest.peerInfo;

        PeerInfo closestPeer = ls.findClosestNeighbor(joiningHexId);
        if(closestPeer != null && isCloser(joiningHexId, closestPeer.getHexID(), myHexID)) { // I havc a closer peer in my ls
            log.info(() -> "Forwarding request to peer in leafset --> " + closestPeer.getHexID());
            forwardRequest(joinRequest, closestPeer);
            return;
        }
        int row = longestMatchingPrefixLength(myHexID, joiningHexId);
        int col = Character.digit(joiningHexId.charAt(row), 16);
        if(row < 4) { // we are not the destination
            if(rt.getPeerInfo(row, col) != null) { // we can make a big jump in rt
                PeerInfo rtPeer = rt.getPeerInfo(row, col);
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
        updateTables(joiningPeerInfo);
        sendJoinResponse(joiningPeerInfo, myHexID, ls, rt);
    }

    private void sendJoinResponse(PeerInfo joinPeerInfo, String myHexID, Leafset ls, RoutingTable rt) {
        List<PeerInfo> leafsetList = ls.getAllPeers();
        leafsetList.remove(joinPeerInfo);
        List<PeerInfo> rtList = rt.getAllPeers();
        rtList.remove(joinPeerInfo);
        JoinResponse joinResponse = new JoinResponse(Protocol.JOIN_RESPONSE, myPeerInfo, myHexID, leafsetList, rtList);
        try {
            TCPConnection conn = peerToConn.get(joinPeerInfo);
            if(conn == null){
                Socket socket = new Socket(joinPeerInfo.getIP(), joinPeerInfo.getPort());
                conn = new TCPConnection(socket, this);
                conn.startReceiverThread();
                socketToConn.put(socket, conn);
                peerToConn.put(joinPeerInfo, conn);
            }
            conn.sender.sendData(joinResponse.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private void forwardRequest(JoinRequest joinRequest, PeerInfo peerInfo) {
        try {
            TCPConnection conn = peerToConn.get(peerInfo);
            if(conn == null){
                Socket socket = new Socket(peerInfo.getIP(), peerInfo.getPort());
                conn = new TCPConnection(socket, this);
                conn.startReceiverThread();
                socketToConn.put(socket, conn);
                peerToConn.put(peerInfo, conn);
            }
            conn.sender.sendData(joinRequest.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private PeerInfo closestOverallPeer(String joiningNodeHexId) {
        PeerInfo closestOverallPeer = null;
        long minDistance = Long.MAX_VALUE;
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

    private void processEntryNode(Event event, Socket socket) {
        EntryNode entryNode = (EntryNode) event;
        log.info(() -> "Received entry node from Discovery: " + entryNode.peerInfo.toString());
        log.info(() -> "Sending join request...");
        sendJoinRequest(entryNode.peerInfo);
    }

    private void sendJoinRequest(PeerInfo entryNode) {
        JoinRequest joinRequest = new JoinRequest(Protocol.JOIN_REQUEST, myPeerInfo);
        try {
            TCPConnection conn = peerToConn.get(entryNode);
            if(conn == null) {
                Socket socket = new Socket(entryNode.getIP(), entryNode.getPort());
                conn = new TCPConnection(socket, this);
                conn.startReceiverThread();
                socketToConn.put(socket, conn);
                peerToConn.put(entryNode, conn);
            }
            conn.sender.sendData(joinRequest.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private void registerResponse(Event event, Socket socket) {
        // log.info(() -> "Received register response from Discovery...");
        // Message responseEvent = (Message) event; 
        // log.info(() -> responseEvent.info);
    }

    private void deregisterResponse(Event event, Socket socket) {
        // log.info(() -> "Received deregister response from Discovery...");
        // Message responseEvent = (Message) event;
        // log.info(() -> responseEvent.info);
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
        try(ServerSocket serverSocket = new ServerSocket(0);) {
            myPeerInfo = new PeerInfo(myHexID, new ConnInfo(InetAddress.getLocalHost().getHostAddress(), serverSocket.getLocalPort()));
            log = Logger.getLogger(Peer.class.getName() + "[" + myPeerInfo.toString() + "]");
            register();

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
                Runnable cmd = commands.get(command);
                if(cmd == null) {
                    log.info(() -> "Please enter a valid command.");
                } else {
                    cmd.run();
                }
            }
        } catch(NullPointerException e) {
            warning.accept(e);
        }
    }

    private void exit() {
        deregister();
        PeerInfo lower = ls.getlower();
        PeerInfo higher = ls.getHigher();
        if(lower != null) {
            sendExitMessage(lower, myPeerInfo, higher);
        }
        if(higher != null) {
            sendExitMessage(higher, myPeerInfo, lower);
        }
        try {
            Thread.sleep(500);
        } catch(InterruptedException e) {
            warning.accept(e);
        }
        System.exit(0);
    }

    private void sendExitMessage(PeerInfo updatePeer, PeerInfo exitingPeer, PeerInfo newNeighbor) {
        log.info(() -> "Sending exit message to --> " + updatePeer.getHexID());
        Exit exitMessage = new Exit(Protocol.EXIT, exitingPeer, newNeighbor);
        try {
            TCPConnection conn = peerToConn.get(updatePeer);
            if(conn == null) {
                Socket socket = new Socket(updatePeer.getIP(), updatePeer.getPort());
                conn = new TCPConnection(socket, this);
                conn.startReceiverThread();
                socketToConn.put(socket, conn);
                peerToConn.put(updatePeer, conn);
            }
            conn.sender.sendData(exitMessage.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private void startCommands() {
        commands.put("exit", this::exit);
        commands.put("id", this::printId);
        commands.put("leaf-set", this::printLeafset);
        commands.put("routing-table", this::printRoutingTable);
        commands.put("print-connections", this::printConnections);
    }

    private void printConnections() {
        for(Map.Entry<PeerInfo, TCPConnection> conn : peerToConn.entrySet()) {
            log.info(() -> conn.getKey().toString());
        }
    }

    private void printRoutingTable() {
        System.out.println(rt);
    }

    private void printLeafset(){
        System.out.print(ls);
    }

    private void printId(){
        System.out.println(myHexID);
    }

    public static void main(String[] args) {

        LogConfig.init(Level.INFO);
        Peer peer;
        peer = (args.length > 2) ? new Peer(args[0], Integer.parseInt(args[1]), args[2]) : new Peer(args[0], Integer.parseInt(args[1]));
        new Thread(peer::startNode, "Node-" + peer.toString() + "-Server").start();
        new Thread(peer::readTerminal, "Node-" + peer.toString() + "-Terminal").start();

    }
}
