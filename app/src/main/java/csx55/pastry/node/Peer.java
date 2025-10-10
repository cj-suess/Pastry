package csx55.pastry.node;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.util.LogConfig;
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

    private String hexID;
    public PeerInfo peerInfo;

    private Map<Socket, TCPConnection> socketToConn = new ConcurrentHashMap<>();

    private Map<Integer, Consumer<Event>> events = new HashMap<>();
    private Map<String, Runnable> commands = new HashMap<>();


    // Leafset
    // Routing Table
    

    public Peer(String host, int port, String hexID) {
        regNodeInfo = new ConnInfo(host, port);
        this.hexID = hexID;
        startsEvents();
        startCommands();
    }

    public Peer(String host, int port){
        regNodeInfo = new ConnInfo(host, port);
        this.hexID = String.format("%04X", ThreadLocalRandom.current().nextInt(65536));
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
            Protocol.DEREGISTER_RESPONSE, this::deregisterResponse
        );
    }

    private void registerResponse(Event event) {
        log.info(() -> "Received register response from Registry...");
        Message responseEvent = (Message) event; 
        log.info(() -> responseEvent.info);
    }

    private void deregisterResponse(Event event) {
        log.info(() -> "Received deregister response from Registry...");
        Message responseEvent = (Message) event;
        log.info(() -> responseEvent.info);
    }

    private void register() {
        try{
            Socket socket = new Socket(regNodeInfo.getIP(), regNodeInfo.getPort());
            regConn = new TCPConnection(socket, this);
            Register registerMessage = new Register(Protocol.REGISTER_REQUEST, peerInfo);
            regConn.startReceiverThread();
            regConn.sender.sendData(registerMessage.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }

    }

    private void deregister() {
        log.info(() -> "Deregistering node...");
            Deregister deregisterRequest = new Deregister(Protocol.DEREGISTER_REQUEST, peerInfo);
            try {
                regConn.sender.sendData(deregisterRequest.getBytes());
            } catch (IOException e) {
                warning.accept(e);
            }
    }

    private void startNode() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            peerInfo = new PeerInfo(hexID, new ConnInfo(InetAddress.getLocalHost().getHostAddress(), serverSocket.getLocalPort()));
            log = Logger.getLogger(Peer.class.getName() + "[" + peerInfo.toString() + "]");
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
