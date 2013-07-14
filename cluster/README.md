# Higgs Cluster

## Implementation not yet published

A cluster is a set of Higgs servers which communicate via a de-centralized pubsub style environment.

A gossip protocol is used to communicate the state of Nodes within the cluster.

All communication between Nodes is done using the Boson protocol, this enables Nodes to be written in any
language. As long as the Node can serialize/deserialize the boson protocol and understands the gossip protocol;
it can be a member of the cluster.

# Joining a cluster

Server or Node requires a "seed" Node to join a cluster. If no seed Node is available then it is assumed
to be the only Node in the cluster. A Node can have multiple seed Nodes.

A seed Node is just another Node within the cluster, it has no control over the node about to join. It's
main purpose is to help the new Node bootstrap by sending it the state of the cluster.

When a new Node joins the cluster, it registers

1. Its role, e.g. database,web-server etc...
2. The events/topics it subscribes to
3. The parameters it accepts for an event
4. Which parameters are required for an event
5. A set of data transformers it has access to

# Events/Topics

Topics are namespaced under the Node's role
e.g. database/save-post means the database server handles the save-post path this means another
non-database Node, can subscribe to save-post and does something completely different without conflict

Multiple servers can register the same role and topics, in such case
the cluster will load balance and automatically use each node fairly
e.g. Two database Nodes subscribe to save-post (database/save-post) in this case because they are under the same
event namespace the cluster treats them as being able to perform the same task and automatically distributes
requests to save posts between them

Any Server or Client can emit an event.
When an event is emitted it can have parameters/data associated with it.
The nodes that subscribe to the given event can choose to accept some or all of the parameters emitted.
What parameters a subscriber accepts is determined by the data types of it's subscription classMethod signature.
Subscribers can also indicate a "strict" mode where a given parameters must be sent (using an annotation for e.g.)

## Data Transformers

A Node registers what are called DataTransformers. A data transformer is capable of accepting data of different
formats and converting them into a format suitable to pass as a parameter to a subscription classMethod.
A data transformer is registered once per node but multiple instances are used. If no data transformer is
registered or the registered ones cannot convert a particular type then the parameter types of the subscription
classMethod must match the parameters being emitted by an event producer (another node or client).
For e.g.
Given 2 Nodes, A and B.
B subscribers to "save-post" and accepts parameters (String user_id,Post post)
Node A wants to emit a "save-post" event but it has the data in a Map.
One of the nodes, A or B must have a data transformer that accepts a Map and outputs a String and a Post type.
If Node A has such a transformer then the conversion happens before emitting the event, if Node B has such a
transformer then the Map is emitted and the conversion happens on Node B before invoking the subscribed classMethod.

Transformers from other nodes (not A or B) cannot be used for the conversion, even if they have such a transformer
i.e. either Node that is involved in emitting or consuming the event can have the transformer but no one else.
 If any other Node were to become involved the data would have to be routed to them and the event would need
 to be proxied to the destination Node. Too much effort for now....

If neither Node has the required transformer then the event producer should immediately be notified (thorwing an exception for e.g.)
and nothing should be emitted.
The decision to trigger an error should be based entirely on the data transformers registered and the param types
of the subscription classMethod. Doing so means no data needs to be sent to determine if a Node can handle a set of parameters
making the entire operation local to the Node producing the event and reducing overall network IO

# Cluster state

Periodically (configurable but maybe every second or two) every Node should emit a "state" event.
The state event has a set of parameters. Amongst the parameters is a merkle tree.
All cluster related events are emitted under the "cluster" namespace.

Where ever consistency needs to be verified in the cluster the merkle tree is used. The merkle tree is a snapshot
view of the entire cluster according to a Node at any given point.

The hierarchy represented by the merkle tree is similar to:

<pre>                                 +----------------+
                +----------------+  Cluster name  +---------------------+
                |                +-------+--------+                     |
                |                        |                              |
          +-----v-----+          +-------v--------+             +-------v-------+
     +----+ Node A    +----+     |  Node B        |       +-----+  Node C       +------+
     |    +-----------+    |     +--------+-------+       |     +---------------+      |
     |                     |              |               |                            |
     |                     |              |               |                            |
     |                     |              |               |                            |
     |                     |              |               |                            |
+----v-------+  +----------v-----+        |         +-----v--------+         +---------v--------+
| Events     |  | Transformers   |        |         | Events       |         |  Transformers    |
+------------+  +----------------+        |         +--------------+         +------------------+
                                          |
                                          |
                         +-------------+  |  +-----------------+
       +-----------------+ Events      &lt;--+--&gt; Transformers    +--------------------+
       |                 +----+--------+     +--------------+--+                    |
       |                      |                             |                       |
 +-----v----+          +------v---+                  +------v-------+       +-------v-------+
 |   E1     |          |   E2     |                  |    T1        |     +-+     T2        |
 +--+------++          ++-------+-+                  +--+-----------+     | +-----------+---+
    |      |            |       |                       |                 |             |
    |      |            |       |                       |                 |             |
 +--v-+ +--v-+       +--v-+  +--v-+              +---|--v+---+        +---v---+   +---+-v-+---+---+
 | P1 | | P2 |       | P1 |  | P2 |              | I1| I2|I3 |        | I1| I2|   | I1| I2| I3| I4|
 |----| |----|       |----|  |----|              |-----------|        |-------|   |---------------|
 | R  | | OP |       | OP |  | OP |              |  O1   |O2 |        |  O1   |   |      O1       |
 +----+ +----+       +----+  +----+              +-------+---+        +-------+   +---------------+


 +----------------------------+
 |      Legend                |
 |----------------------------|
 |E1-n  = Event               |
 |T1-n  = Transformers        |
 |P1-n  = Event Parameter     |
 |R     = Required Parameter  |
 |OP    = Optional Parameter  |
 |I1-n  = Transformer input   |
 |O1-n  = Transformer output  |
 +----------------------------+
 </pre>
 Awesome ASCII diagram created with http://www.asciiflow.com/ (https://github.com/lewish/asciiflow)

 NOTE: The diagram is not a complete view of the merkle tree to be sent, just an example of typical Nodes
 so there may be more branches

For example, one obvious dataset that needs to be checked regularly is the set of Nodes in the cluster and information about each Node.
A merkle tree in this case is used to represent a Node's view of the cluster, as shown in the diagram above.

The state event emitted by every Node should contain a merkle tree describing the cluster as it is known to the
emitting Node. When this merkle tree is received it is checked and compared to a local merkle tree.

If the local and remote views of the cluster differs then the information required to update is sent to the
node with the older, incorrect view. Thus preventing the entire view of the cluster from being sent every time.

