package configs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import graph.Message;

/**
 * A vertex in the {@link Graph} directed graph model.
 *
 * <p>Each node has a name, an optional {@link Message} payload, and a list of outgoing
 * edges to other nodes. Used for graph visualization and cycle detection.</p>
 *
 * @see Graph
 * @see graph.Message
 */
public class Node {
    private final String name;
    private final List<Node> edges;
    private Message msg;

    /**
     * Creates a new graph node with the given name.
     *
     * @param name the unique node identifier (e.g. {@code "TA"} for topic A,
     *             {@code "PlusAgent"} for an agent)
     */
    public Node(String name) {
        this.name = name;
        this.edges = new ArrayList<>();
        this.msg = null;
    }

    /**
     * Returns the name of this node.
     *
     * @return the node name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the list of outgoing edges from this node.
     *
     * @return the mutable adjacency list of target nodes
     */
    public List<Node> getEdges() {
        return edges;
    }

    /**
     * Adds a directed edge from this node to the target node.
     *
     * <p>Duplicate edges to the same target are ignored.</p>
     *
     * @param to the destination node; ignored if {@code null}
     */
    public void addEdge(Node to) {
        if (to != null && !edges.contains(to)) {
            edges.add(to);
        }
    }

    /**
     * Sets the message payload associated with this node.
     *
     * @param m the message to store, or {@code null} to clear
     */
    public void setMessage(Message m) {
        this.msg = m;
    }

    /**
     * Returns the message payload associated with this node.
     *
     * @return the stored {@link Message}, or {@code null} if none is set
     */
    public Message getMessage() {
        return msg;
    }

    /**
     * Detects whether a directed cycle is reachable from this node.
     *
     * <p>Uses DFS with visited and recursion-stack tracking.</p>
     *
     * @return {@code true} if a cycle exists reachable from this node; {@code false} otherwise
     */
    public boolean hasCycles() {
        Set<Node> visited = new HashSet<>();
        Set<Node> stack = new HashSet<>();
        return hasCyclesDfs(this, visited, stack);
    }

    private boolean hasCyclesDfs(Node n, Set<Node> visited, Set<Node> stack) {
        // 'stack' holds the nodes on the current recursion path; revisiting one means a
        // back-edge, i.e. a cycle.
        if (stack.contains(n)) {
            return true;
        }
        // Already fully explored from a previous branch and found clean: no need to recurse again.
        if (visited.contains(n)) {
            return false;
        }
        visited.add(n);
        stack.add(n);
        for (Node nb : n.getEdges()) {
            if (hasCyclesDfs(nb, visited, stack)) {
                return true;
            }
        }
        // Leaving this node: pop it off the active path so sibling branches aren't misjudged.
        stack.remove(n);
        return false;
    }

    /**
     * Returns a string representation of this node.
     *
     * @return a string in the form {@code Node(name)}
     */
    @Override
    public String toString() {
        return "Node(" + name + ")";
    }
}
