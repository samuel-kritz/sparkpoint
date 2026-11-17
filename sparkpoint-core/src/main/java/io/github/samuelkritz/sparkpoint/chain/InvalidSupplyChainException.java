package io.github.samuelkritz.sparkpoint.chain;

import java.util.List;

/**
 * Thrown when nodes and routes do not form a usable supply chain. Carries every problem found, not
 * just the first, so a user can fix a file in one pass.
 */
public final class InvalidSupplyChainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // Exceptions are Serializable, but List is not guaranteed to be. SparkPoint never serializes
    // exceptions, and the full text of every problem is in the message, so nothing is lost.
    private final transient List<String> problems;

    InvalidSupplyChainException(List<String> problems) {
        super("invalid supply chain (" + problems.size() + (problems.size() == 1 ? " problem" : " problems") + "): "
                + String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    /** Every problem found, in the order nodes and routes were given. */
    public List<String> problems() {
        return problems;
    }
}
