package io.github.samuelkritz.sparkpoint.chain;

/**
 * What kind of place a node is. Descriptive only: it is shown to the user and may be used to draw
 * the map, but no rule or calculation behaves differently because of it.
 */
public enum NodeType {
    /** Provides an input to production, such as a beekeeper supplying pollination hives. */
    SUPPLIER,
    FARM,
    /** Hullers, packers, mills and other plants that transform the product. */
    PROCESSOR,
    /** Warehouses, cold stores and other storage. */
    WAREHOUSE,
    PORT,
    /** The receiving end of the chain, such as a restaurant or retailer. */
    CUSTOMER,
    OTHER
}
