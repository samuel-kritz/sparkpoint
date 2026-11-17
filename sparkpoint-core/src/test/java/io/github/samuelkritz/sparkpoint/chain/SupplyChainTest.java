package io.github.samuelkritz.sparkpoint.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.samuelkritz.sparkpoint.geo.GeoPoint;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SupplyChainTest {

    // A small almond chain. Bees and the orchard feed the huller; the processor feeds two customers.
    //
    //   bees ----> orchard ----> huller ----> processor ----> port
    //                                              \--------> restaurant
    private static final Node BEES = node("bees", "Seattle Apiaries", NodeType.SUPPLIER, 47.6062, -122.3321);
    private static final Node ORCHARD = node("orchard", "Chico Almond Orchard", NodeType.FARM, 39.7285, -121.8375);
    private static final Node HULLER = node("huller", "Orland Huller", NodeType.PROCESSOR, 39.7474, -122.1964);
    private static final Node PROCESSOR =
            node("processor", "Sacramento Processor", NodeType.PROCESSOR, 38.5816, -121.4944);
    private static final Node PORT = node("port", "Port of Oakland", NodeType.PORT, 37.7955, -122.2792);
    private static final Node RESTAURANT =
            node("restaurant", "San Francisco Restaurant", NodeType.CUSTOMER, 37.7749, -122.4194);

    private static final List<Node> ALMOND_NODES = List.of(BEES, ORCHARD, HULLER, PROCESSOR, PORT, RESTAURANT);
    private static final List<Route> ALMOND_ROUTES = List.of(
            route("bees", "orchard"),
            route("orchard", "huller"),
            route("huller", "processor"),
            route("processor", "port"),
            route("processor", "restaurant"));

    private static final SupplyChain ALMONDS = SupplyChain.of(ALMOND_NODES, ALMOND_ROUTES);

    @Nested
    class AValidChain {

        @Test
        void keepsNodesAndRoutesInTheOrderTheyWereGiven() {
            assertThat(ALMONDS.nodes()).containsExactlyElementsOf(ALMOND_NODES);
            assertThat(ALMONDS.routes()).containsExactlyElementsOf(ALMOND_ROUTES);
        }

        @Test
        void findsANodeByItsId() {
            assertThat(ALMONDS.node(id("huller"))).contains(HULLER);
        }

        @Test
        void hasNoNodeForAnUnknownId() {
            assertThat(ALMONDS.node(id("nowhere"))).isEmpty();
        }

        @Test
        void listsTheNodesThatFeedDirectlyIntoANode() {
            assertThat(ALMONDS.upstreamOf(id("huller"))).containsExactly(id("orchard"));
            assertThat(ALMONDS.upstreamOf(id("bees"))).isEmpty();
        }

        @Test
        void listsTheNodesANodeFeedsDirectly() {
            assertThat(ALMONDS.downstreamOf(id("processor"))).containsExactly(id("port"), id("restaurant"));
            assertThat(ALMONDS.downstreamOf(id("port"))).isEmpty();
        }

        @Test
        void listsEverythingDownstreamOfANodeNearestFirst() {
            assertThat(ALMONDS.allDownstreamOf(id("orchard")))
                    .containsExactly(id("huller"), id("processor"), id("port"), id("restaurant"));
            assertThat(ALMONDS.allDownstreamOf(id("restaurant"))).isEmpty();
        }

        @Test
        void listsANodeReachedByTwoPathsOnlyOnce() {
            // a feeds b and c, and both feed d.
            SupplyChain diamond = SupplyChain.of(
                    List.of(plain("a"), plain("b"), plain("c"), plain("d")),
                    List.of(route("a", "b"), route("a", "c"), route("b", "d"), route("c", "d")));

            assertThat(diamond.allDownstreamOf(id("a"))).containsExactly(id("b"), id("c"), id("d"));
            assertThat(diamond.upstreamOf(id("d"))).containsExactly(id("b"), id("c"));
        }

        @Test
        void ordersNodesSoThatEveryNodeComesAfterEverythingThatFeedsIt() {
            assertThat(ALMONDS.upstreamFirst()).containsExactly(BEES, ORCHARD, HULLER, PROCESSOR, PORT, RESTAURANT);
        }

        @Test
        void ordersUpstreamFirstEvenWhenTheFileListsNodesDownstreamFirst() {
            SupplyChain listedBackwards = SupplyChain.of(
                    List.of(plain("shop"), plain("warehouse"), plain("farm")),
                    List.of(route("warehouse", "shop"), route("farm", "warehouse")));

            assertThat(listedBackwards.upstreamFirst())
                    .extracting(Node::id)
                    .containsExactly(id("farm"), id("warehouse"), id("shop"));
        }

        @Test
        void placesANodeFedByTwoOthersAfterBothOfThem() {
            // d is listed first but is fed by b and c, which are both fed by a.
            SupplyChain diamond = SupplyChain.of(
                    List.of(plain("d"), plain("c"), plain("b"), plain("a")),
                    List.of(route("a", "b"), route("a", "c"), route("b", "d"), route("c", "d")));

            assertThat(diamond.upstreamFirst())
                    .extracting(Node::id)
                    .containsExactly(id("a"), id("b"), id("c"), id("d"));
        }

        @Test
        void aSingleNodeWithNoRoutesIsAValidChain() {
            SupplyChain lonely = SupplyChain.of(List.of(plain("farm")), List.of());

            assertThat(lonely.nodes()).hasSize(1);
            assertThat(lonely.upstreamFirst()).hasSize(1);
        }

        @Test
        void refusesQuestionsAboutNodesItDoesNotContain() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ALMONDS.upstreamOf(id("nowhere")))
                    .withMessageContaining("nowhere");
            assertThatIllegalArgumentException().isThrownBy(() -> ALMONDS.downstreamOf(id("nowhere")));
            assertThatIllegalArgumentException().isThrownBy(() -> ALMONDS.allDownstreamOf(id("nowhere")));
        }

        @Test
        void cannotBeChangedThroughTheListsItHandsOut() {
            assertThat(ALMONDS.nodes()).isUnmodifiable();
            assertThat(ALMONDS.routes()).isUnmodifiable();
            assertThat(ALMONDS.upstreamFirst()).isUnmodifiable();
            assertThat(ALMONDS.upstreamOf(id("huller"))).isUnmodifiable();
            assertThat(ALMONDS.downstreamOf(id("huller"))).isUnmodifiable();
            assertThat(ALMONDS.allDownstreamOf(id("huller"))).isUnmodifiable();
        }
    }

    @Nested
    class AnInvalidChain {

        @Test
        void mustHaveAtLeastOneNode() {
            assertThat(problemsWith(List.of(), List.of())).containsExactly("a supply chain needs at least one node");
        }

        @Test
        void cannotUseTheSameNodeIdTwice() {
            List<Node> nodes = List.of(plain("farm"), plain("shop"), plain("farm"));

            assertThat(problemsWith(nodes, List.of())).containsExactly("node id 'farm' is used more than once");
        }

        @Test
        void cannotHaveARouteFromAnUnknownNode() {
            List<Route> routes = List.of(route("ghost", "shop"));

            assertThat(problemsWith(List.of(plain("shop")), routes))
                    .containsExactly("route ghost -> shop starts at unknown node 'ghost'");
        }

        @Test
        void cannotHaveARouteToAnUnknownNode() {
            List<Route> routes = List.of(route("farm", "ghost"));

            assertThat(problemsWith(List.of(plain("farm")), routes))
                    .containsExactly("route farm -> ghost ends at unknown node 'ghost'");
        }

        @Test
        void cannotListTheSameRouteTwice() {
            List<Route> routes = List.of(route("farm", "shop"), route("farm", "shop"));

            assertThat(problemsWith(List.of(plain("farm"), plain("shop")), routes))
                    .containsExactly("route farm -> shop is listed more than once");
        }

        @Test
        void cannotContainACycle() {
            List<Node> nodes = List.of(plain("a"), plain("b"), plain("c"));
            List<Route> routes = List.of(route("a", "b"), route("b", "c"), route("c", "a"));

            assertThat(problemsWith(nodes, routes))
                    .containsExactly("routes form a cycle, so nothing in it comes first: a -> b -> c -> a");
        }

        @Test
        void namesOnlyTheNodesInTheCycleNotTheOnesAroundIt() {
            // farm feeds into a two-node loop, which feeds shop. Only x and y are in the cycle.
            List<Node> nodes = List.of(plain("farm"), plain("x"), plain("y"), plain("shop"));
            List<Route> routes = List.of(route("farm", "x"), route("x", "y"), route("y", "x"), route("y", "shop"));

            assertThat(problemsWith(nodes, routes))
                    .singleElement()
                    .asString()
                    .contains("cycle")
                    .contains("x -> y -> x")
                    .doesNotContain("farm")
                    .doesNotContain("shop");
        }

        @Test
        void reportsEveryProblemAtOnce() {
            List<Node> nodes = List.of(plain("farm"), plain("farm"), plain("shop"));
            List<Route> routes = List.of(route("farm", "ghost"), route("farm", "shop"), route("farm", "shop"));

            assertThat(problemsWith(nodes, routes))
                    .containsExactly(
                            "node id 'farm' is used more than once",
                            "route farm -> ghost ends at unknown node 'ghost'",
                            "route farm -> shop is listed more than once");
        }

        @Test
        void putsEveryProblemInTheExceptionMessage() {
            InvalidSupplyChainException thrown = catchThrowableOfType(
                    InvalidSupplyChainException.class,
                    () -> SupplyChain.of(List.of(plain("farm"), plain("farm")), List.of(route("farm", "ghost"))));

            assertThat(thrown)
                    .hasMessageContaining("2 problems")
                    .hasMessageContaining("node id 'farm' is used more than once")
                    .hasMessageContaining("unknown node 'ghost'");
        }

        @Test
        void rejectsNullCollections() {
            assertThatNullPointerException().isThrownBy(() -> SupplyChain.of(null, List.of()));
            assertThatNullPointerException().isThrownBy(() -> SupplyChain.of(List.of(plain("farm")), null));
        }

        private static List<String> problemsWith(List<Node> nodes, List<Route> routes) {
            InvalidSupplyChainException thrown =
                    catchThrowableOfType(InvalidSupplyChainException.class, () -> SupplyChain.of(nodes, routes));
            assertThat(thrown).as("expected the chain to be rejected").isNotNull();
            return thrown.problems();
        }
    }

    @Nested
    class TheBuildingBlocks {

        @Test
        void aNodeIdCannotBeBlank() {
            assertThatIllegalArgumentException().isThrownBy(() -> new NodeId(""));
            assertThatIllegalArgumentException().isThrownBy(() -> new NodeId("   "));
            assertThatNullPointerException().isThrownBy(() -> new NodeId(null));
        }

        @Test
        void aNodeIdPrintsAsItsValue() {
            assertThat(id("huller")).hasToString("huller");
        }

        @Test
        void aNodeNeedsEveryField() {
            GeoPoint somewhere = new GeoPoint(0, 0);

            assertThatNullPointerException().isThrownBy(() -> new Node(null, "Farm", NodeType.FARM, somewhere));
            assertThatNullPointerException().isThrownBy(() -> new Node(id("farm"), null, NodeType.FARM, somewhere));
            assertThatNullPointerException().isThrownBy(() -> new Node(id("farm"), "Farm", null, somewhere));
            assertThatNullPointerException().isThrownBy(() -> new Node(id("farm"), "Farm", NodeType.FARM, null));
        }

        @Test
        void aNodeNameCannotBeBlank() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Node(id("farm"), " ", NodeType.FARM, new GeoPoint(0, 0)));
        }

        @Test
        void aRouteNeedsBothEnds() {
            assertThatNullPointerException().isThrownBy(() -> new Route(null, id("shop")));
            assertThatNullPointerException().isThrownBy(() -> new Route(id("farm"), null));
        }

        @Test
        void aRouteCannotLeadBackToWhereItStarted() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> route("farm", "farm"))
                    .withMessageContaining("farm");
        }

        @Test
        void aRoutePrintsAsAnArrow() {
            assertThat(route("farm", "shop")).hasToString("farm -> shop");
        }
    }

    private static NodeId id(String value) {
        return new NodeId(value);
    }

    private static Node node(String id, String name, NodeType type, double latitude, double longitude) {
        return new Node(new NodeId(id), name, type, new GeoPoint(latitude, longitude));
    }

    /** A node where only the id matters to the test. */
    private static Node plain(String id) {
        return node(id, id, NodeType.OTHER, 0, 0);
    }

    private static Route route(String from, String to) {
        return new Route(new NodeId(from), new NodeId(to));
    }
}
