package io.github.samuelkritz.sparkpoint.chain;

import java.util.Objects;

/**
 * The identifier a supply-chain file gives a node, used by routes to refer to it.
 *
 * <p>A dedicated type rather than a bare {@code String} so that an id cannot be confused with a
 * node's display name.
 */
public record NodeId(String value) {

    public NodeId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("a node id cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
