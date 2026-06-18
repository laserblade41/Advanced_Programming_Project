package configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import graph.*;
import graph.TopicManagerSingleton.TopicManager;

public class Graph extends ArrayList<Node> {

    /**
     * Build a bipartite-like directed graph from topics and agents:
     * - For every Topic t, create a topic-node named "T" + t.name
     * - For every Agent that appears in subs or pubs, create an agent-node using agent.getName()
     * - For every subscription (topic.subs contains agent): add edge Ttopic -> Aagent
     * - For every publish registration (topic.pubs contains agent): add edge Aagent -> Ttopic
     */
    public void createFromTopics(){
        this.clear();
        TopicManager tm = TopicManagerSingleton.get();
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
     * Graph-level cycle detection over all nodes in this Graph.
     * Uses DFS with colors (0=unvisited,1=visiting,2=done).
     */
    public boolean hasCycles() {
        Map<Node, Integer> state = new HashMap<>();
        for (Node n : this) {
            state.put(n, 0);
        }
        for (Node n : this) {
            if (state.get(n) == 0) {
                if (dfsHasCycle(n, state)) return true;
            }
        }
        return false;
    }

    private boolean dfsHasCycle(Node node, Map<Node, Integer> state) {
        state.put(node, 1); // visiting
        for (Node neigh : node.getEdges()) {
            int st = state.getOrDefault(neigh, 0);
            if (st == 1) {
                return true;
            } else if (st == 0) {
                if (dfsHasCycle(neigh, state)) return true;
            }
        }
        state.put(node, 2); // done
        return false;
    }
}
