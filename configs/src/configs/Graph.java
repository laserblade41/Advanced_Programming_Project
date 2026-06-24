package configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import graph.*;
import graph.TopicManagerSingleton.TopicManager;

/**
 * Directed graph model representing the wiring between topics and agents.
 *
 * <p>Extends {@link java.util.ArrayList} of {@link Node} objects. Populated by
 * {@link #createFromTopics()}, which reads the live topic registry from
 * {@link graph.TopicManagerSingleton} and builds a bipartite structure:</p>
 * <ul>
 *   <li>Topic nodes are named {@code "T"} + topic name (e.g. {@code "TA"})</li>
 *   <li>Agent nodes use {@link graph.Agent#getName()}</li>
 *   <li>Subscription edges run Topic → Agent; publish edges run Agent → Topic</li>
 * </ul>
 *
 * @see Node
 * @see views.HtmlGraphWriter
 */
public class Graph extends ArrayList<Node> {

    /**
     * Rebuilds this graph from the current state of {@link graph.TopicManagerSingleton}.
     *
     * <p>Clears any existing nodes and edges, then creates topic nodes, agent nodes, and
     * directed edges reflecting subscriptions and publisher registrations.</p>
     */
    public void createFromTopics(){
        // Snapshot the live pub-sub state into a static, visualizable bipartite graph:
        // topic-nodes and agent-nodes, with edges encoding subscribe/publish direction.
        this.clear();
        TopicManager tm = TopicManagerSingleton.get();
        // Maps node name -> Node so each topic/agent is created once and reused when adding edges.
        Map<String, Node> nodesByName = new HashMap<>();

        Collection<Topic> topics = tm.getAllTopics().values();

        // create nodes for topics
        for (Topic t : topics) {
            String nodeName = "T" + t.name;
            Node tn = new Node(nodeName);
            nodesByName.put(nodeName, tn);
            this.add(tn);
        }

        // create nodes for agents (found in subs and pubs)
        for (Topic t : topics) {
            for (Agent a : t.getSubs()) {
                String an = a.getName();
                if (!nodesByName.containsKey(an)) {
                    Node anode = new Node(an);
                    nodesByName.put(an, anode);
                    this.add(anode);
                }
            }
            for (Agent a : t.getPubs()) {
                String an = a.getName();
                if (!nodesByName.containsKey(an)) {
                    Node anode = new Node(an);
                    nodesByName.put(an, anode);
                    this.add(anode);
                }
            }
        }

        // add edges: Topic -> Agent for subs, Agent -> Topic for pubs
        for (Topic t : topics) {
            Node tnode = nodesByName.get("T" + t.name);
            if (tnode == null) continue;

            for (Agent a : t.getSubs()) {
                Node anode = nodesByName.get(a.getName());
                if (anode != null) {
                    tnode.addEdge(anode);
                }
            }
            for (Agent a : t.getPubs()) {
                Node anode = nodesByName.get(a.getName());
                if (anode != null) {
                    anode.addEdge(tnode);
                }
            }
        }
    }

    /**
     * Detects whether this graph contains a cycle using depth-first search.
     *
     * <p>Uses a three-color DFS (unvisited, visiting, done) across all nodes in the graph.</p>
     *
     * @return {@code true} if a directed cycle exists; {@code false} otherwise
     */
    public boolean hasCycles() {
        // Three-color DFS. Colors: 0 = unvisited (white), 1 = on current DFS path (gray),
        // 2 = fully explored (black). A back-edge to a gray node proves a cycle.
        Map<Node, Integer> state = new HashMap<>();
        for (Node n : this) {
            state.put(n, 0);
        }
        // Start a DFS from every still-white node so disconnected sub-graphs are all covered.
        for (Node n : this) {
            if (state.get(n) == 0) {
                if (dfsHasCycle(n, state)) return true;
            }
        }
        return false;
    }

    private boolean dfsHasCycle(Node node, Map<Node, Integer> state) {
        state.put(node, 1); // mark gray: node is now on the active recursion path
        for (Node neigh : node.getEdges()) {
            int st = state.getOrDefault(neigh, 0);
            if (st == 1) {
                // Neighbor is gray -> we looped back onto our own path: a cycle exists.
                return true;
            } else if (st == 0) {
                if (dfsHasCycle(neigh, state)) return true;
            }
            // st == 2 (black) is a safe, already-finished node: skip it.
        }
        state.put(node, 2); // mark black: this node and all its descendants are cycle-free
        return false;
    }
}
