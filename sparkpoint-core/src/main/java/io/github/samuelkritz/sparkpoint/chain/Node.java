package io.github.samuelkritz.sparkpoint.chain;

import io.github.samuelkritz.sparkpoint.geo.GeoPoint;
import java.util.Objects;

/**
 * A place in the supply chain.
 *
 * @param id       unique within the chain
 * @param name     what the user calls this place
 * @param type     what kind of place it is
 * @param location where it is
 */
public record Node(NodeId id, String name, NodeType type, GeoPoint location) {

    public Node {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(location, "location");
        if (name.isBlank()) {
            throw new IllegalArgumentException("node '" + id + "' needs a name");
        }
    }
}
