package csx55.pastry.node;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.transport.TCPSender;
import csx55.pastry.util.LogConfig;
import csx55.pastry.wireformats.*;

public class Discover implements Node {
    
    private Logger log = Logger.getLogger(this.getClass().getName());
    
    private int port;
    private ServerSocket serverSocket;
    private boolean running = true;

    private List<TCPConnection> openConnections = new ArrayList<>();
    private Map<PeerInfo, TCPConnection> peerToConnMap = new ConcurrentHashMap<>();

    public Discover(int port) {
        this.port = port;
    }

    @Override
    public void onEvent(Event event, Socket socket) {
        TCPSender sender = getSender(openConnections, socket);
        if(event.getType() == Protocol.REGISTER_REQUEST) {
            log.info("Register request detected. Checking status...");
            Register registerEvent = (Register) event;
            if (!peerToConnMap.containsKey(registerEvent.peerInfo)) {
                sendRegisterSuccess(registerEvent, sender, socket);
            }
            else {
                sendRegisterFailure(registerEvent, sender);
            }
        }

    }

    public void readTerminal() {
        try(Scanner scanner = new Scanner(System.in)) {
            while(running) {
                String command = scanner.nextLine();
                String[] splitCommand = command.split("\\s+");
                switch (splitCommand[0]) {
                    default:
                        log.warning("Unknown terminal command...");
                        break;
                }
            }
        }
    }

    private void startDiscovery() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("Discovery node is up and running. Listening on port: " + port);
            while(running) {
                Socket clientSocket = serverSocket.accept();
                InetSocketAddress client = (InetSocketAddress) clientSocket.getRemoteSocketAddress();
                log.info("New connection from: " + client.getAddress() + ":" + client.getPort());
                TCPConnection conn = new TCPConnection(clientSocket, this);
                conn.startReceiverThread();
                openConnections.add(conn);
            }
        } catch(IOException e) {
            log.warning("Exception while accepting connections..." + e.getStackTrace());
        }
    }

    private TCPSender getSender(List<TCPConnection> openConnections, Socket socket) {
        TCPSender sender = null;
        for(TCPConnection conn : openConnections) {
            if(socket == conn.socket) {
                sender = conn.sender;
            }
        }
        return sender;
    }

    private void sendRegisterSuccess(Register registerEvent, TCPSender sender, Socket socket) {
        try {
            TCPConnection conn = getConnectionBySocket(openConnections, socket);
            peerToConnMap.put(registerEvent.peerInfo, conn);
            log.info(() -> "New node was added to the registry successfully!\n" + "\tCurrent number of nodes in registry: " + peerToConnMap.size());
            String info = "Registration request successful. The number of messaging nodes currently constituting the overlay is (" + peerToConnMap.size() + ")";
            Message successMessage = new Message(Protocol.REGISTER_RESPONSE, (byte)0, info);
            log.info(() -> "Sending success response to messaging node at " + registerEvent.peerInfo.conn.toString());
            sender.sendData(successMessage.getBytes());
        } catch(IOException e) {
            log.warning("Exception while registering compute node..." + e.getStackTrace());
        }
    }

    private void sendRegisterFailure(Register registerEvent, TCPSender sender) {
        try {
            if(peerToConnMap.containsKey(registerEvent.peerInfo)) {
                log.info(() -> "Cannot register node. A matching nodeID already exists in the registry...");
                String info = "Registration request failed.";
                Message failureMessage = new Message(Protocol.REGISTER_RESPONSE, (byte)1, info);
                sender.sendData(failureMessage.getBytes());
            }
        } catch(IOException e) {
            log.warning("Exception while registering compute node..." + e.getStackTrace());
        }
    }

    private TCPConnection getConnectionBySocket(List<TCPConnection> openConnections, Socket socket) {
        for(TCPConnection conn : openConnections) {
            if(conn.socket == socket) {
                return conn;
            }
        }
        return null;
    }

    // store registered nodes (16-bit identifier, NodeID, nickname)

    // detect collision

    // return random registered node

    public static void main(String[] args) {
        LogConfig.init(Level.INFO);
        Discover discovery = new Discover(Integer.parseInt(args[0]));
        new Thread(discovery::startDiscovery).start();
        new Thread(discovery::readTerminal).start();
    }
    
}
