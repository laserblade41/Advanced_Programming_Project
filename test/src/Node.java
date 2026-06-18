package test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Node {
    private final String name;
    private final List<Node> edges;
    private Message msg;

    public Node(String name) {
        this.name = name;
        this.edges = new ArrayList<>();
        this.msg = null;
    }

    public String getName() {
        return name;
    }

    public List<Node> getEdges() {
        return edges;
    }

    public void addEdge(Node to) {
        if (to != null && !edges.contains(to)) {
            edges.add(to);
        }
    }

    public void setMessage(Message m) {
        this.msg = m;
    }

    public Message getMessage() {
        return msg;
    }

    /**
     * Instance-level cycle detection: returns true if there is a cycle reachable from this node.
     */
    public boolean hasCycles() {
        Set<Node> visited = new HashSet<>();
        Set<Node> stack = new HashSet<>();
        return hasCyclesDfs(this, visited, stack);
    }

    private boolean hasCyclesDfs(Node n, Set<Node> visited, Set<Node> stack) {
        if (stack.contains(n)) {
            return true;
        }
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
        stack.remove(n);
        return false;
    }

    @Override
    public String toString() {
        return "Node(" + name + ")";
    }
}