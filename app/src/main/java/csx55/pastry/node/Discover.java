package csx55.pastry.node;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.util.LogConfig;
import csx55.pastry.wireformats.*;

public class Discover implements Node {
    
    private Logger log = Logger.getLogger(this.getClass().getName());
    
    private int port;
    private ServerSocket serverSocket;
    private boolean running = true;

    private List<TCPConnection> openConnections = new ArrayList<>();

    public Discover(int port) {
        this.port = port;
    }

    @Override
    public void onEvent(Event event, Socket socket) {
        
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
