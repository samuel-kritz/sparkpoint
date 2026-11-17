package io.github.samuelkritz.sparkpoint.chain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A validated, immutable supply chain: a directed graph of nodes joined by routes, with no cycles.
 *
 * <p>"Upstream" means towards the origin of the goods and "downstream" towards the customer. A
 * disruption travels downstream, so the graph must have no cycles: in a cycle every node would be
 * downstream of itself and nothing could be evaluated first.
 */
public final class SupplyChain {

    private final Map<NodeId, Node> nodesById;
    private final List<Node> nodes;
    private final List<Route> routes;
    private final Map<NodeId, List<NodeId>> upstream;
    private final Map<NodeId, List<NodeId>> downstream;
    private final List<Node> upstreamFirst;

    private SupplyChain(
            Map<NodeId, Node> nodesById,
            List<Route> routes,
            Map<NodeId, List<NodeId>> upstream,
            Map<NodeId, List<NodeId>> downstream,
            List<NodeId> upstreamFirst) {
        this.nodesById = nodesById;
        this.nodes = List.copyOf(nodesById.values());
        this.routes = List.copyOf(routes);
        this.upstream = immutableCopy(upstream);
        this.downstream = immutableCopy(downstream);
        this.upstreamFirst = upstreamFirst.stream().map(nodesById::get).toList();
    }

    /**
     * Builds a supply chain, checking that it is usable.
     *
     * @throws InvalidSupplyChainException if there are no nodes, a node id is repeated, a route
     *     refers to a node that does not exist or is listed twice, or the routes form a cycle
     */
    public static SupplyChain of(Collection<Node> nodes, Collection<Route> routes) {
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(routes, "routes");
        List<String> problems = new ArrayList<>();

        if (nodes.isEmpty()) {
            problems.add("a supply chain needs at least one node");
        }

        Map<NodeId, Node> nodesById = new LinkedHashMap<>();
        Set<NodeId> repeatedIds = new HashSet<>();
        for (Node node : nodes) {
            boolean alreadyPresent = nodesById.putIfAbsent(node.id(), node) != null;
            if (alreadyPresent && repeatedIds.add(node.id())) {
                problems.add("node id '" + node.id() + "' is used more than once");
            }
        }

        Map<NodeId, List<NodeId>> upstream = new HashMap<>();
        Map<NodeId, List<NodeId>> downstream = new HashMap<>();
        for (NodeId id : nodesById.keySet()) {
            upstream.put(id, new ArrayList<>());
            downstream.put(id, new ArrayList<>());
        }

        Set<Route> usableRoutes = new LinkedHashSet<>();
        Set<Route> repeatedRoutes = new HashSet<>();
        for (Route route : routes) {
            boolean fromIsKnown = nodesById.containsKey(route.from());
            boolean toIsKnown = nodesById.containsKey(route.to());
            if (!fromIsKnown) {
                problems.add("route " + route + " starts at unknown node '" + route.from() + "'");
            }
            if (!toIsKnown) {
                problems.add("route " + route + " ends at unknown node '" + route.to() + "'");
            }
            if (!fromIsKnown || !toIsKnown) {
                continue;
            }
            if (!usableRoutes.add(route)) {
                if (repeatedRoutes.add(route)) {
                    problems.add("route " + route + " is listed more than once");
                }
                continue;
            }
            downstream.get(route.from()).add(route.to());
            upstream.get(route.to()).add(route.from());
        }

        List<NodeId> upstreamFirst = orderUpstreamFirst(nodesById.keySet(), upstream, downstream);
        if (upstreamFirst.size() < nodesById.size()) {
            Set<NodeId> unordered = new LinkedHashSet<>(nodesById.keySet());
            upstreamFirst.forEach(unordered::remove);
            problems.add("routes form a cycle, so nothing in it comes first: " + describeCycle(unordered, upstream));
        }

        if (!problems.isEmpty()) {
            throw new InvalidSupplyChainException(problems);
        }
        return new SupplyChain(nodesById, List.copyOf(usableRoutes), upstream, downstream, upstreamFirst);
    }

    /** Every node, in the order given. */
    public List<Node> nodes() {
        return nodes;
    }

    /** Every route, in the order given. */
    public List<Route> routes() {
        return routes;
    }

    public Optional<Node> node(NodeId id) {
        return Optional.ofNullable(nodesById.get(id));
    }

    /**
     * The nodes that feed directly into the given node.
     *
     * @throws IllegalArgumentException if the node is not in this chain
     */
    public List<NodeId> upstreamOf(NodeId id) {
        return upstream.get(requireKnown(id));
    }

    /**
     * The nodes the given node feeds directly.
     *
     * @throws IllegalArgumentException if the node is not in this chain
     */
    public List<NodeId> downstreamOf(NodeId id) {
        return downstream.get(requireKnown(id));
    }

    /**
     * Every node that depends on the given node, directly or through other nodes, nearest first.
     * The node itself is not included.
     *
     * @throws IllegalArgumentException if the node is not in this chain
     */
    public List<NodeId> allDownstreamOf(NodeId id) {
        requireKnown(id);
        // Breadth-first search. The set both records the order and stops a node reached by two paths
        // from being visited twice.
        Set<NodeId> reached = new LinkedHashSet<>();
        Deque<NodeId> toVisit = new ArrayDeque<>(downstream.get(id));
        while (!toVisit.isEmpty()) {
            NodeId next = toVisit.removeFirst();
            if (reached.add(next)) {
                toVisit.addAll(downstream.get(next));
            }
        }
        return List.copyOf(reached);
    }

    /**
     * Every node, ordered so that each one comes after all the nodes that feed it (a topological
     * order). Evaluating nodes in this order means a node's upstream statuses are always known
     * before its own is worked out.
     */
    public List<Node> upstreamFirst() {
        return upstreamFirst;
    }

    private NodeId requireKnown(NodeId id) {
        if (!nodesById.containsKey(id)) {
            throw new IllegalArgumentException("no node '" + id + "' in this supply chain");
        }
        return id;
    }

    /**
     * Kahn's algorithm: repeatedly take a node with nothing unprocessed feeding it. If the result is
     * shorter than the node list, the nodes left over are in, or downstream of, a cycle.
     */
    private static List<NodeId> orderUpstreamFirst(
            Collection<NodeId> ids, Map<NodeId, List<NodeId>> upstream, Map<NodeId, List<NodeId>> downstream) {
        Map<NodeId, Integer> unprocessedFeeders = new HashMap<>();
        Deque<NodeId> ready = new ArrayDeque<>();
        for (NodeId id : ids) {
            int feeders = upstream.get(id).size();
            unprocessedFeeders.put(id, feeders);
            if (feeders == 0) {
                ready.add(id);
            }
        }

        List<NodeId> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            NodeId next = ready.removeFirst();
            ordered.add(next);
            for (NodeId fed : downstream.get(next)) {
                if (unprocessedFeeders.merge(fed, -1, Integer::sum) == 0) {
                    ready.add(fed);
                }
            }
        }
        return ordered;
    }

    /**
     * Finds one cycle among the nodes Kahn's algorithm could not order and writes it as
     * {@code a -> b -> c -> a}.
     *
     * <p>Every unordered node is fed by at least one other unordered node (otherwise it would have
     * been ordered), so walking backwards from any of them must eventually revisit a node. The
     * stretch between the two visits is a cycle.
     */
    private static String describeCycle(Set<NodeId> unordered, Map<NodeId, List<NodeId>> upstream) {
        List<NodeId> walkedBackwards = new ArrayList<>();
        NodeId current = unordered.iterator().next();
        while (!walkedBackwards.contains(current)) {
            walkedBackwards.add(current);
            current = upstream.get(current).stream()
                    .filter(unordered::contains)
                    .findFirst()
                    .orElseThrow();
        }

        List<NodeId> cycle = walkedBackwards.subList(walkedBackwards.indexOf(current), walkedBackwards.size());
        // The walk went against the routes, so read it in reverse to follow them.
        StringBuilder description = new StringBuilder(current.toString());
        for (int i = cycle.size() - 1; i >= 0; i--) {
            description.append(" -> ").append(cycle.get(i));
        }
        return description.toString();
    }

    private static Map<NodeId, List<NodeId>> immutableCopy(Map<NodeId, List<NodeId>> lists) {
        Map<NodeId, List<NodeId>> copy = new HashMap<>();
        lists.forEach((id, list) -> copy.put(id, List.copyOf(list)));
        return Map.copyOf(copy);
    }
}
