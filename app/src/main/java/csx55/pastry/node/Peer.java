package csx55.pastry.node;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.util.*;
import csx55.pastry.wireformats.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.*;
import java.util.stream.Stream;

public class Peer implements Node {

    private Logger log = Logger.getLogger(this.getClass().getName());
    private final Consumer<Exception> warning = e -> log.log(Level.WARNING, e.getMessage(), e);

    private final Object lock = new Object();
    private final Converter c = Converter.getConverter();

    private boolean running = true;
    private final ConnInfo regNodeInfo;
    private TCPConnection regConn;

    private final String myHexID;
    public PeerInfo myPeerInfo;
    Leafset ls;
    RoutingTable rt;
    private Path dataDir;

    private final Map<Socket, TCPConnection> socketToConn = new ConcurrentHashMap<>();
    private final Map<String, TCPConnection> peerToConn = new ConcurrentHashMap<>();
    private final Set<PeerInfo> references = ConcurrentHashMap.newKeySet();

    private Map<Integer, BiConsumer<Event, Socket>> events = new HashMap<>();
    private final Map<String, Runnable> commands = new HashMap<>();

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
            if(handler != null) {
                handler.accept(event, socket);
            }
        }
    }

    private void startsEvents() {
        events.put(Protocol.REGISTER_RESPONSE, this::registerResponse);
        events.put(Protocol.DEREGISTER_RESPONSE, this::deregisterResponse);
        events.put(Protocol.ENTRY_NODE, this::processEntryNode);
        events.put(Protocol.JOIN_REQUEST, this::processJoinRequest);
        events.put(Protocol.JOIN_RESPONSE, this::processJoinResponse);
        events.put(Protocol.LEAFSET_UPDATE, this::processLeafsetUpdate);
        events.put(Protocol.EXIT, this::processExit);
        events.put(Protocol.REFERENCE, this::processReference);
        events.put(Protocol.REFERENCE_NOTIFICATION, this::processReferenceRemoval);
        events.put(Protocol.HANDSHAKE, this::processHandshake);
        events.put(Protocol.ROUTING_UPDATE, this::processRoutingUpdate);
        events.put(Protocol.STORE_REQUEST, this::processStoreRequest);
        events.put(Protocol.RETRIEVE_REQUEST, this::processRetrieveRequest);
    }

    private void processRetrieveRequest(Event event, Socket socket) {
        RetrieveRequest retrieveRequest = (RetrieveRequest) event;
        log.info(() -> "Received retrieve request...");
        String fileName = retrieveRequest.getFileName();
        String fileHex = c.convertBytesToHex(Converter.hash16(fileName));
        retrieveRequest.getRoutingPath().add(myHexID);

        PeerInfo closestPeer = closestOverallPeer(fileHex);
        if (closestPeer != null && isCloser(fileHex, closestPeer.getHexID(), myHexID)) {
            log.info(() -> "Forwarding request to closer peer -> " + closestPeer.getHexID());
            send(closestPeer, retrieveRequest);
            return;
        }

        log.info(() -> "Retrieving data. Sending retrieve response back to data node...");
        RetrieveResponse retrieveResponse = new RetrieveResponse(Protocol.RETRIEVE_RESPONSE, retrieveRequest.getRoutingPath());
        send(retrieveRequest.getDataNode(), retrieveResponse);
    }

    private void processStoreRequest(Event event, Socket socket) {
        StoreRequest storeRequest = (StoreRequest) event;
        log.info(() -> "Received store request...");
        String fileName = storeRequest.getFileName();
        String fileHex = c.convertBytesToHex(Converter.hash16(fileName));
        byte[] data = storeRequest.getData();
        storeRequest.getRoutingPath().add(myHexID);

        PeerInfo closestPeer = closestOverallPeer(fileHex);
        if (closestPeer != null && isCloser(fileHex, closestPeer.getHexID(), myHexID)) {
            log.info(() -> "Forwarding request to closer peer -> " + closestPeer.getHexID());
            send(closestPeer, storeRequest);
            return;
        }

        log.info(() -> "Storing data. Sending store response back to data node...");
        storeData(fileName, data);
        StoreResponse storeResponse = new StoreResponse(Protocol.STORE_RESPONSE, storeRequest.getRoutingPath());
        send(storeRequest.getDataNode(), storeResponse);
    }

    private void storeData(String fileName, byte[] data) {
        try {
            Path path = dataDir.resolve(fileName);
            Files.write(path, data);
        } catch (IOException e) {
            warning.accept(e);
        }
    }

    private void processRoutingUpdate(Event event, Socket socket) {
        RoutingUpdate routingUpdate = (RoutingUpdate) event;
        log.info(() -> "Received routing update...");
        // add whatever peers I can to my rt
        synchronized(lock) {
            for(PeerInfo p : routingUpdate.getRoutingList()) {
                if(p.getHexID().equals(myHexID)) { continue; }
                int row = longestMatchingPrefixLength(myHexID, p.getHexID());
                int col = Character.digit(p.getHexID().charAt(row), 16);
                if(rt.addPeer(p, row, col)) {
                    log.info(() -> "Adding peer " + p.getHexID());
                    sendReferenceMessage(p);
                }
            }
        }
    }

    private void processLeafsetUpdate(Event event, Socket socket) {
        LeafsetUpdate leafsetUpdate = (LeafsetUpdate) event;
        PeerInfo sourcPeerInfo = leafsetUpdate.getPeerInfo();
        int role = leafsetUpdate.getRole();

        log.info(() -> "Received leafset update from --> " + sourcPeerInfo.getHexID());

        synchronized(lock) {
            if(role == LeafsetUpdate.LOWER) { ls.setLower(sourcPeerInfo); }
            if(role == LeafsetUpdate.HIGHER) { ls.setHigher(sourcPeerInfo); }
        }
    }

    private void processHandshake(Event event, Socket socket) {
        Register handshake = (Register) event;
        PeerInfo peer = handshake.peerInfo;
        TCPConnection conn = socketToConn.get(socket);
        if(conn != null) {
            peerToConn.put(peer.getHexID(), conn);
        }
    }

    private TCPConnection getConnection(PeerInfo peerInfo) {
        String peerHexId = peerInfo.getHexID();
        TCPConnection conn;
        synchronized(peerToConn) {
            conn = peerToConn.get(peerHexId);
            if(conn != null) { return conn; }
            try{
                Socket socket = new Socket(peerInfo.getIP(), peerInfo.getPort());
                conn = new TCPConnection(socket, this);
                conn.startReceiverThread();
                synchronized(peerToConn) {
                    socketToConn.put(socket, conn);
                    peerToConn.put(peerHexId, conn);
                }
                Register handshake = new Register(Protocol.HANDSHAKE, myPeerInfo);
                conn.sender.sendData(handshake.getBytes());
                return conn;
            }catch(IOException e) {
                warning.accept(e);
            }
        }
        return null;
    }

    private void processReferenceRemoval(Event event, Socket socket) {
        Register referenceRemoval = (Register) event;
        synchronized(lock) {
            rt.remove(referenceRemoval.peerInfo);
        }
    }

    private void processReference(Event event, Socket socket) {
        Register referenceMessage = (Register) event;
        synchronized(lock) {
            references.add(referenceMessage.peerInfo);
        }
    }

    private void processExit(Event event, Socket socket) {
        Exit exitMessage = (Exit) event;
        PeerInfo exitingPeer = exitMessage.getExitingPeer();
        PeerInfo newNeighbor =  exitMessage.getNewNeighbor();
        synchronized(lock) {
            rt.remove(exitingPeer);
            ls.remove(exitingPeer);
            if(exitMessage.getRole() == Exit.LOWER) { ls.setLower(newNeighbor); }
            if(exitMessage.getRole() == Exit.HIGHER) { ls.setHigher(newNeighbor); }
        }
    }

    private void processJoinResponse(Event event, Socket socket) {
        JoinResponse joinResponse = (JoinResponse) event;
        PeerInfo respondingPeer = joinResponse.getRespondingPeer();
        log.info(() -> "Received join response from --> " + respondingPeer.toString());

        List<PeerInfo> rtCopy;

        synchronized(lock) {
            // update rt with responding peer's applicable entries
            for(PeerInfo p : joinResponse.getRoutingTable()){
                if(!p.getHexID().equals(myHexID)){
                    int row = longestMatchingPrefixLength(myHexID, p.getHexID());
                    int col = Character.digit(p.getHexID().charAt(row), 16);
                    if(rt.addPeer(p, row, col)) { sendReferenceMessage(p); }
                }
            }
            //  aslo update rt with the responding peer
            int row = longestMatchingPrefixLength(myHexID, respondingPeer.getHexID());
            int col = Character.digit(respondingPeer.getHexID().charAt(row), 16);
            if(rt.addPeer(respondingPeer, row, col)) { sendReferenceMessage(respondingPeer); }

            // trying to build a leafset take 808
            int responderRole = joinResponse.getResponderRole();
            List<PeerInfo> respondersOldLeafset = joinResponse.getLeafset();
            long responderVal = Long.parseLong(respondingPeer.getHexID(), 16);

            if(responderRole == JoinResponse.LOWER) { // this is my lower
                ls.setLower(respondingPeer);
                // find the furthest peer in their leafset cw to get my higher
                PeerInfo higher = null;
                long minDist = Long.MAX_VALUE;
                for(PeerInfo p : respondersOldLeafset){
                    if(p.equals(respondingPeer)) { continue; }
                    long dist = ls.clockwise(responderVal, Long.parseLong(p.getHexID(), 16));
                    if(dist < minDist) {
                        minDist = dist;
                        higher = p;
                    }
                }
                ls.setHigher(higher);
            } else { // this is my higher
                ls.setHigher(respondingPeer);
                // look for the furthest ccw peer to get my lower
                PeerInfo lower = null;
                long minDist = Long.MAX_VALUE;
                for(PeerInfo p : respondersOldLeafset){
                    if(p.equals(respondingPeer)) { continue; }
                    long dist = ls.clockwise(Long.parseLong(p.getHexID(), 16), responderVal);
                    if(dist < minDist) {
                        minDist = dist;
                        lower = p;
                    }
                }
                ls.setLower(lower);
            }
            // complete ring when only two peers exist
            if(ls.getLower() != null && ls.getHigher() == null) {
                ls.setHigher(ls.getLower());
            }
            if(ls.getLower() == null && ls.getHigher() != null) {
                ls.setLower(ls.getHigher());
            }

            // copy updated rt
            rtCopy = rt.getAllPeers();
        }
        // send updated rt to peers so they can add what they can
        sendRoutingUpdate(new RoutingUpdate(Protocol.ROUTING_UPDATE, rtCopy));
    }

    private void sendRoutingUpdate(RoutingUpdate routingUpdate) {
        List<PeerInfo> peers;
        synchronized(lock) {
            peers = rt.getAllPeers();
            peers.addAll(ls.getAllPeers());
        }
        for(PeerInfo p : peers) {
            send(p, routingUpdate);
        }
    }

    private void sendReferenceMessage(PeerInfo peerInfo){
        send(peerInfo, new Register(Protocol.REFERENCE, myPeerInfo));
    }

    private void processJoinRequest(Event event, Socket socket) {
        JoinRequest joinRequest = (JoinRequest) event;
        log.info(() -> "Received join request for --> " + joinRequest.peerInfo.toString());
        PeerInfo joiningPeerInfo = joinRequest.peerInfo;

        PeerInfo closestPeer = closestOverallPeer(joiningPeerInfo.getHexID());
        if (closestPeer != null && isCloser(joiningPeerInfo.getHexID(), closestPeer.getHexID(), myHexID)) {
            log.info(() -> "Forwarding request to closer peer -> " + closestPeer.getHexID());
            send(closestPeer, joinRequest);
            return;
        }

        log.info(() -> "I am the closest peer. Sending join response to --> " + joiningPeerInfo.getHexID());

        List<PeerInfo> lsCopy;
        List<PeerInfo> rtCopy;
        int responderPosition;

        synchronized(lock) {
            lsCopy = ls.getAllPeers();
            rtCopy = rt.getAllPeers();
        }

        long myVal = Long.parseLong(myHexID, 16);
        long joiningVal = Long.parseLong(joiningPeerInfo.getHexID(), 16);
        long cwDist = ls.clockwise(myVal, joiningVal);
        long ccwDist = ls.clockwise(joiningVal, myVal);

        // now determine in which direction am I closer to the joining node
        if (cwDist < ccwDist) { // joining peer is on my high side
            responderPosition = JoinResponse.LOWER; // I'm lower relative to the joiner
            synchronized (lock) {
                PeerInfo oldHigher = ls.getHigher();
                ls.setHigher(joiningPeerInfo);

                // update my old higher that the joiner is now its lower neighbor
                if (oldHigher != null) {
                    LeafsetUpdate update = new LeafsetUpdate(Protocol.LEAFSET_UPDATE, joiningPeerInfo, LeafsetUpdate.LOWER);
                    send(oldHigher, update);
                }

                if (ls.getLower() == null) {
                    ls.setLower(joiningPeerInfo);
                }
            }
        } else { // joining peer is on my low side
            responderPosition = JoinResponse.HIGHER; // I'm higher relative to the joiner
            synchronized (lock) {
                PeerInfo oldLower = ls.getLower();
                ls.setLower(joiningPeerInfo);

                // update my old lower that the joiner is now its higher neighbor
                if (oldLower != null) {
                    LeafsetUpdate update = new LeafsetUpdate(Protocol.LEAFSET_UPDATE, joiningPeerInfo, LeafsetUpdate.HIGHER);
                    send(oldLower, update);
                }

                if (ls.getHigher() == null) {
                    ls.setHigher(joiningPeerInfo);
                }
            }
        }

        int row = longestMatchingPrefixLength(myHexID, joiningPeerInfo.getHexID());
        int col = Character.digit(joiningPeerInfo.getHexID().charAt(row), 16);

        // send join response
        synchronized(lock) {
            rt.addPeer(joiningPeerInfo, row, col);
        }
        JoinResponse joinResponse = new JoinResponse(Protocol.JOIN_RESPONSE, myPeerInfo, responderPosition, myHexID, lsCopy, rtCopy);
        send(joiningPeerInfo, joinResponse);
        sendReferenceMessage(joiningPeerInfo);
    }

    private PeerInfo closestOverallPeer(String joiningNodeHexId) {
        PeerInfo closestOverallPeer = null;
        long minDistance = Long.MAX_VALUE;
        long joiningNodeVal = Long.parseLong(joiningNodeHexId, 16);
        List<PeerInfo> allPeers;
        synchronized(lock) {
            allPeers = new ArrayList<>(rt.getAllPeers());
            allPeers.addAll(ls.getAllPeers());
        }
        for(PeerInfo peer : allPeers) {
            if (peer.getHexID().equals(myHexID)) continue;
            long currPeerVal = Long.parseLong(peer.getHexID(), 16);
            long distance = ls.calculateMinDistance(joiningNodeVal, currPeerVal);
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
        return ls.calculateMinDistance(Long.parseLong(joiningHexId, 16), Long.parseLong(closestPeerHexId, 16)) < ls.calculateMinDistance(Long.parseLong(joiningHexId, 16), Long.parseLong(myHexId, 16));
    }

    private void processEntryNode(Event event, Socket socket) {
        EntryNode entryNode = (EntryNode) event;
        log.info(() -> "Received entry node from Discovery: " + entryNode.peerInfo.toString());
        log.info(() -> "Sending join request...");
        sendJoinRequest(entryNode.peerInfo);
    }

    private void sendJoinRequest(PeerInfo entryNode) {
        JoinRequest joinRequest = new JoinRequest(Protocol.JOIN_REQUEST, myPeerInfo);
        send(entryNode, joinRequest);
    }

    private void send(PeerInfo peerInfo, Event event) {
        try {
            TCPConnection conn = getConnection(peerInfo);
            conn.sender.sendData(event.getBytes());
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

    @Override
    public void startNode() {
        try(ServerSocket serverSocket = new ServerSocket(0)) {
            myPeerInfo = new PeerInfo(myHexID, new ConnInfo(InetAddress.getLocalHost().getHostAddress(), serverSocket.getLocalPort()));
            log = Logger.getLogger(Peer.class.getName() + "[" + myPeerInfo.toString() + "]");
            register();

            // create directory in tmp
            dataDir = Paths.get("/tmp/" + myHexID);
            Files.createDirectories(dataDir);

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
        running = false;
        deregister();
        synchronized(lock) {
            PeerInfo lower = ls.getLower();
            PeerInfo higher = ls.getHigher();
            if(lower != null) {
                sendExitMessage(lower, myPeerInfo, higher, Exit.HIGHER);
            }
            if(higher != null) {
                sendExitMessage(higher, myPeerInfo, lower, Exit.LOWER);
            }
            notifyReferences();
            log.info(() -> "Exit complete...");
        }
        for(TCPConnection conn : peerToConn.values()) {
            try {
                conn.socket.close();
            } catch (IOException e) {
                warning.accept(e);
            }
        }
        try{
            Files.deleteIfExists(dataDir);
        } catch(IOException e) {
            warning.accept(e);
        }
        System.exit(0);
    }

    private void notifyReferences() {
        Register notifyReferencesMessage = new Register(Protocol.REFERENCE_NOTIFICATION, myPeerInfo);
        for(PeerInfo p : references) {
            send(p, notifyReferencesMessage);
        }
    }

    private void sendExitMessage(PeerInfo updatePeer, PeerInfo exitingPeer, PeerInfo newNeighbor, int role) {
        log.info(() -> "Sending exit message to --> " + updatePeer.getHexID());
        Exit exitMessage = new Exit(Protocol.EXIT, exitingPeer, newNeighbor, role);
        send(updatePeer, exitMessage);
    }

    private void startCommands() {
        commands.put("exit", this::exit);
        commands.put("id", this::printId);
        commands.put("leaf-set", this::printLeafset);
        commands.put("routing-table", this::printRoutingTable);
        commands.put("print-connections", this::printConnections);
        commands.put("list-files", this::listFiles);
    }

    private void listFiles() {
        try(Stream<Path> stream = Files.list(dataDir)){
            stream.forEach(file -> {
                System.out.println(file.getFileName() + ", " + c.convertBytesToHex(Converter.hash16(file.getFileName().toString())));
            });
        } catch (IOException e) {
            warning.accept(e);
        }
    }

    private void printConnections() {
        for(String conn : peerToConn.keySet()) {
            log.info(() -> conn);
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

        LogConfig.init(Level.WARNING);
        Peer peer;
        peer = (args.length > 2) ? new Peer(args[0], Integer.parseInt(args[1]), args[2]) : new Peer(args[0], Integer.parseInt(args[1]));
        new Thread(peer::startNode, "Node-" + peer.toString() + "-Server").start();
        new Thread(peer::readTerminal, "Node-" + peer.toString() + "-Terminal").start();

    }
}
