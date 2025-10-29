package csx55.pastry.node;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.*;

import csx55.pastry.transport.TCPConnection;
import csx55.pastry.transport.TCPSender;
import csx55.pastry.util.LogConfig;
import csx55.pastry.wireformats.*;

public class Discover implements Node {
    
    private Logger log = Logger.getLogger(this.getClass().getName());
    private final Consumer<Exception> warning = e -> log.log(Level.WARNING, e.getMessage(), e);  

    private int port;
    private ServerSocket serverSocket;
    private boolean running = true;
    private Random rand = new Random();

    private Map<PeerInfo, TCPConnection> peerToConnMap = new ConcurrentHashMap<>();
    private Map<Socket, TCPConnection> socketToConn = new ConcurrentHashMap<>();
    
    private Map<String, Runnable> commands = new HashMap<>();
    private Map<Integer, BiConsumer<Event, Socket>> events = new HashMap<>();

    public Discover(int port) {
        this.port = port;
        startEvents();
        startCommands();
    }

    @Override
    public void onEvent(Event event, Socket socket) {
        if(event != null) {
            BiConsumer<Event, Socket> handler = events.get(event.getType());
            handler.accept(event, socket);
        } else {
             warning.accept(null);
        }
    }

    private void startEvents() {
        events = Map.of(
            Protocol.REGISTER_REQUEST, this::registerRequest,
            Protocol.DEREGISTER_REQUEST, this::deregisterRequest,
            Protocol.ENTRY_NODE, this::dataEntryNode
        );
    }

    private void dataEntryNode(Event event, Socket socket) {
        log.info(() -> "Responding to data entry node request...");
        TCPConnection conn = socketToConn.get(socket);
        TCPSender sender = conn.getSender();
        sendEntryNode(sender);
    }

    private void registerRequest(Event event, Socket socket) {
        log.info("Register request detected. Checking status...");
        TCPConnection conn = socketToConn.get(socket);
        TCPSender sender = conn.getSender();
        Register registerEvent = (Register) event;
        if (!peerToConnMap.containsKey(registerEvent.peerInfo)) {
            if(!peerToConnMap.isEmpty()) {
                log.info(() -> "Sending entry node to " + registerEvent.peerInfo.getHexID());
                sendEntryNode(sender);
            }
            peerToConnMap.put(registerEvent.peerInfo, conn);
            sendSuccess(registerEvent, sender);
        }
        else {
            sendFailure(registerEvent, sender);
        }
    }

    private void sendEntryNode(TCPSender sender) {
        try {
            EntryNode entryNodeMessage = new EntryNode(Protocol.ENTRY_NODE, getRandomEntryNode());
            log.info(() -> "Sending " + entryNodeMessage.peerInfo.getHexID());
            sender.sendData(entryNodeMessage.getBytes());
        } catch(IOException e) {
            warning.accept(e);
        }
    }

    private PeerInfo getRandomEntryNode() {
        List<PeerInfo> list = new ArrayList<>(peerToConnMap.keySet());
        return list.get(rand.nextInt(list.size()));
    }

    private void deregisterRequest(Event event, Socket socket) {
        log.info("Deregister request detected. Checking status...");
        TCPConnection conn = socketToConn.get(socket);
        TCPSender sender = conn.getSender();
        Deregister deregisterEvent = (Deregister) event;
        if (peerToConnMap.containsKey(deregisterEvent.peerInfo)) {
            peerToConnMap.remove(deregisterEvent.peerInfo);
            sendSuccess(deregisterEvent, sender);
        }
        else {
            sendFailure(deregisterEvent, sender);
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

    private void startCommands() {
        commands.put("list-nodes", this::listNodes);
    }

    private void listNodes() {
        peerToConnMap.keySet().forEach(key -> {
            System.out.println(key.toString());
        });
    }

    @Override
    public void startNode() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("Discovery node is up and running. Listening on port: " + port);
            while(running) {
                Socket clientSocket = serverSocket.accept();
                InetSocketAddress client = (InetSocketAddress) clientSocket.getRemoteSocketAddress();
                log.info("New connection from: " + client.getAddress() + ":" + client.getPort());
                TCPConnection conn = new TCPConnection(clientSocket, this);
                conn.startReceiverThread();
                socketToConn.put(clientSocket, conn);
            }
        } catch(IOException e) {
            log.log(Level.WARNING, "Exception while starting discovery node...", e);
        }
    }

    private void sendSuccess(Event event, TCPSender sender) {
        try {
            if(event.getType() == Protocol.REGISTER_REQUEST) {
                Register registerEvent = (Register) event;
                log.info(() -> registerEvent.peerInfo.getHexID() + " was added to the registry successfully!\n" + "\tCurrent number of nodes in registry: " + peerToConnMap.size());
                String info = "Registration request successful. The number of messaging nodes currently constituting the overlay is (" + peerToConnMap.size() + ")";
                Message successMessage = new Message(Protocol.REGISTER_RESPONSE, info);
                sender.sendData(successMessage.getBytes());
            }
            else if(event.getType() == Protocol.DEREGISTER_REQUEST){
                Deregister deregisterEvent = (Deregister) event;
                log.info(() -> deregisterEvent.peerInfo.getHexID() + " was removed from the registry successfully!\n" + "\tCurrent number of nodes in registry: " + peerToConnMap.size());
                String info = "Deregistration request successful. The number of messaging nodes currently constituting the overlay is (" + peerToConnMap.size() + ")";
                Message successMessage = new Message(Protocol.DEREGISTER_RESPONSE, info);
                sender.sendData(successMessage.getBytes());
            }
        } catch(IOException e) {
            log.log(Level.WARNING, "Exception while registering compute node...", e);
        }
    }

    private void sendFailure(Event event, TCPSender sender) {
        try {
            if(event.getType() == Protocol.REGISTER_REQUEST) {
                log.info(() -> "Cannot register node. A matching node already exists in the registry...");
                String info = "Registration request failed.";
                Message failureMessage = new Message(Protocol.REGISTER_RESPONSE, info);
                sender.sendData(failureMessage.getBytes());
            }
            else if(event.getType() == Protocol.DEREGISTER_REQUEST) {
                log.info(() -> "Cannot deregister node. The node does not exist in the registry...");
                String info = "Deregistration request failed.";
                Message failureMessage = new Message(Protocol.DEREGISTER_RESPONSE, info);
                sender.sendData(failureMessage.getBytes());
            }
        } catch(IOException e) {
            log.log(Level.WARNING, "Exception while registering compute node...", e);
        }
    }

    public static void main(String[] args) {
        LogConfig.init(Level.WARNING);
        Discover discovery = new Discover(Integer.parseInt(args[0]));
        new Thread(discovery::startNode).start();
        new Thread(discovery::readTerminal).start();
    }
    
}
