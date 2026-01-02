# Pastry (Implementing the Pastry P2P System)

The objective was to build a simple peer to peer network where individual peers have 16-bit identifiers. This project has several sub-items associated with it: this relates to constructing
the logical overlay and traversing the network efficiently to store and retrieve content. Below is a high-level overview of the main components:


1. A discovery node that maintains information about the list of peers in the system. Every time a peer joins or exits the system it notifies this discovery node.
2. Protocol for routing content in the P2P Network:
   The primary functionality provided by the DHT is the lookup operation. A lookup(key) operation identifies the node with the numerically closest identifier to the key. The routing algorithm      for the DHT involves two data structures that assist in routing: (1) Leaf Set and (2) Routing table.
3. 
