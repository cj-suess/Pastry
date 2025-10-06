package csx55.pastry.wireformats;

import java.io.IOException;

public interface Event {
    int getType();
    byte[] getBytes() throws IOException;
}
