package csx55.pastry.node;

import java.net.Socket;
import csx55.pastry.wireformats.Event;

public interface Node {

    void onEvent(Event event, Socket socket);

    void startNode();
}
