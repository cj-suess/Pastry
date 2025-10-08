package csx55.pastry.node;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.util.LogConfig;
import csx55.pastry.wireformats.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class Peer implements Node {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private boolean running = true;

    private ServerSocket serverSocket;
    
    private ConnInfo regNodeInfo;
    private TCPConnection regConn;

    private ConnInfo myConnInfo;
    private String hexID;
    public PeerInfo peerInfo;

    private Map<Socket, TCPConnection> socketToConn = new ConcurrentHashMap<>();
    // Leafset
    // Routing Table
    

    public Peer(String host, int port, String hexID) {
        regNodeInfo = new ConnInfo(host, port);
        this.hexID = hexID;
    }

    public Peer(String host, int port){
        regNodeInfo = new ConnInfo(host, port);
        this.hexID = String.format("%04X", ThreadLocalRandom.current().nextInt(65536));
    }


    @Override
    public void onEvent(Event event, Socket socket) {
        
    }

    private void register() {
        try{
            Socket socket = new Socket(regNodeInfo.getIP(), regNodeInfo.getPort());
            regConn = new TCPConnection(socket, this);
            Register registerMessage = new Register(Protocol.REGISTER_REQUEST, peerInfo);
            regConn.startReceiverThread();
            regConn.sender.sendData(registerMessage.getBytes());
        } catch(IOException e) {
            log.warning("Exception thrown while registering node with Registry..." + e.getStackTrace().toString());
        }

    }

    private void deregister() {
        log.info("Deregistering node...");
            Deregister deregisterRequest = new Deregister(Protocol.DEREGISTER_REQUEST, myConnInfo);
            try {
                regConn.sender.sendData(deregisterRequest.getBytes());
            } catch (IOException e) {
                log.warning("Exception while deregitering node..." + e.getMessage());
            }
    }

    private void startNode() {
        try {
            serverSocket = new ServerSocket(0);
            myConnInfo = new ConnInfo(InetAddress.getLocalHost().getHostAddress(), serverSocket.getLocalPort());
            peerInfo = new PeerInfo(hexID, myConnInfo, hexID);
            log = Logger.getLogger(Peer.class.getName() + "[" + myConnInfo.toString() + "]");
            register();
            while(running) {
                Socket clientSocket = serverSocket.accept();
                log.info("New connection from: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                TCPConnection conn = new TCPConnection(clientSocket, this);
                conn.startReceiverThread();
                socketToConn.put(clientSocket, conn);
            }
        }catch(IOException e) {
            log.warning("Exception in startNode...." + e.getStackTrace());
        }
    }

    private void readTerminal() {
        try(Scanner scanner = new Scanner(System.in)) {
            while(true) {
                String command = scanner.nextLine();
                switch (command) {
                    case "exit":
                        deregister();
                    default:
                        break;
                }
            }
        } 
    }

    // exit method

    public static void main(String[] args) {

        LogConfig.init(Level.INFO);
        Peer peer;
        peer = (args.length > 2) ? new Peer(args[0], Integer.parseInt(args[1]), args[2]) : new Peer(args[0], Integer.parseInt(args[1]));
        new Thread(peer::startNode, "Node-" + peer.toString() + "-Server").start();
        new Thread(peer::readTerminal, "Node-" + peer.toString() + "-Terminal").start();

    }

}
