# Pastry (Implementing the Pastry P2P System)

The objective was to build a simple peer-to-peer network where individual peers have 16-bit identifiers. This project has several sub-items associated with it: this relates to constructing the logical overlay and traversing the network efficiently to store and retrieve content.

## High-Level Overview

Below is a high-level overview of the main components:

1.  **Discovery Node**
    * Maintains information about the list of peers in the system.
    * Every time a peer joins or exits the system, it notifies this discovery node.

2.  **Protocol for Routing Content**
    * The primary functionality provided by the DHT is the `lookup` operation.
    * A `lookup(key)` operation identifies the node with the numerically closest identifier to the key.
    * The routing algorithm for the DHT involves two data structures that assist in routing:
        1.  **Leaf Set**
        2.  **Routing Table**

3.  **Leaf Set**
    * At a given peer, this data structure is responsible for tracking the $2l$ neighbors of that peer: $l$ to the right of the peer and $l$ to its left.
    * The DHT ID space can be thought of as being organized as a ring: $0$ to $2^{16}-1$.
    * The neighbors refer to peers whose identifiers are numerically closest to the peer in question.

4.  **Routing Table**
    * The routing table maintains information about several peers in the system.
    * All peer identifiers in the DHT are viewed as hexadecimal values.
    * The routing table classifies peer identifiers based on their hexadecimal prefixes.

5.  **Addition of a New Peer (Joining Protocol)**
    * New nodes use a joining protocol to acquire their routing table and leaf set contents.
    * This protocol also includes notifying other nodes of changes that they must make to their tables.

6.  **Storing Data Items**
    * The `StoreData` program contacts a random peer (retrieving this information from the Discovery node) to initiate the data storage process.
    * The `StoreData` program will first compute the 16-bit digest for the file name and then use this hash to lookup the peer where it should be stored.
    * The node that responds will be the node that is most suitable to store the data. The file is then transferred to that suitable peer, which is responsible for storing the file in the appropriate directory of the machine that it is running on.
