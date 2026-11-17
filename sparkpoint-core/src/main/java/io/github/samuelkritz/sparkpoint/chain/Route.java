package io.github.samuelkritz.sparkpoint.chain;

import java.util.Objects;

/**
 * Goods flow from one node to another: {@code to} depends on {@code from}.
 *
 * @param from the upstream node
 * @param to   the downstream node
 */
public record Route(NodeId from, NodeId to) {

    public Route {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.equals(to)) {
            throw new IllegalArgumentException("a route cannot lead from node '" + from + "' back to itself");
        }
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
