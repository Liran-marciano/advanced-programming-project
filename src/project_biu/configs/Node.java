package project_biu.configs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import project_biu.graph.Message;

/**
 * Vertex in a directed graph that represents the runtime topology of a
 * publish/subscribe computational graph.
 *
 * <p>Each {@code Node} carries a name, a list of outgoing {@code edges} to
 * other nodes, and an optional {@link Message} (named {@code msg} to match
 * the course skeleton). Cycle detection lives on the node itself:
 * {@link #hasCycles()} returns {@code true} when the subgraph reachable
 * from this node contains a back edge.
 */
public class Node {

    private String name;
    private List<Node> edges;
    private Message msg;

    public Node(String name) {
        this.name = name;
        this.edges = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Node> getEdges() {
        return edges;
    }

    public void setEdges(List<Node> edges) {
        this.edges = edges;
    }

    public Message getMsg() {
        return msg;
    }

    public void setMsg(Message msg) {
        this.msg = msg;
    }

    /** Appends {@code n} to the list of outgoing edges. */
    public void addEdge(Node n) {
        edges.add(n);
    }

    /**
     * Returns {@code true} if the subgraph reachable from this node contains
     * a directed cycle.
     *
     * <p>Uses classical DFS with two sets: {@code visited} for nodes already
     * fully explored, and {@code stack} for nodes currently on the recursion
     * path. Hitting a node already on the stack proves a back edge — i.e. a
     * cycle.
     */
    public boolean hasCycles() {
        return hasCyclesFrom(new HashSet<>(), new HashSet<>());
    }

    private boolean hasCyclesFrom(Set<Node> visited, Set<Node> stack) {
        if (stack.contains(this)) return true;
        if (visited.contains(this)) return false;
        visited.add(this);
        stack.add(this);
        for (Node next : edges) {
            if (next.hasCyclesFrom(visited, stack)) return true;
        }
        stack.remove(this);
        return false;
    }
}
