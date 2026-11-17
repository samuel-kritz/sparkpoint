package io.github.samuelkritz.sparkpoint.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.samuelkritz.sparkpoint.geo.GeoPoint;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SupplyChainXmlReaderTest {

    @Nested
    class AValidFile {

        @Test
        void becomesASupplyChainWithTheSameNodesAndRoutes() {
            SupplyChain chain = read("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Chico Orchard" type="farm" latitude="39.7285" longitude="-121.8375"/>
                            <node id="shop" name="Corner Shop" type="customer" latitude="37.7749" longitude="-122.4194"/>
                        </nodes>
                        <routes>
                            <route from="farm" to="shop"/>
                        </routes>
                    </supplyChain>
                    """);

            assertThat(chain.nodes())
                    .containsExactly(
                            new Node(new NodeId("farm"), "Chico Orchard", NodeType.FARM, new GeoPoint(39.7285, -121.8375)),
                            new Node(new NodeId("shop"), "Corner Shop", NodeType.CUSTOMER, new GeoPoint(37.7749, -122.4194)));
            assertThat(chain.routes()).containsExactly(new Route(new NodeId("farm"), new NodeId("shop")));
        }

        @Test
        void understandsEveryNodeType() {
            SupplyChain chain = read("""
                    <supplyChain>
                        <nodes>
                            <node id="a" name="A" type="supplier" latitude="0" longitude="0"/>
                            <node id="b" name="B" type="farm" latitude="0" longitude="0"/>
                            <node id="c" name="C" type="processor" latitude="0" longitude="0"/>
                            <node id="d" name="D" type="warehouse" latitude="0" longitude="0"/>
                            <node id="e" name="E" type="port" latitude="0" longitude="0"/>
                            <node id="f" name="F" type="customer" latitude="0" longitude="0"/>
                            <node id="g" name="G" type="other" latitude="0" longitude="0"/>
                        </nodes>
                    </supplyChain>
                    """);

            // Every enum constant, in declaration order: fails if a type is added to one side only.
            assertThat(chain.nodes()).extracting(Node::type).containsExactly(NodeType.values());
        }

        @Test
        void mayLeaveOutRoutesAltogether() {
            SupplyChain chain = read("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="farm" latitude="1" longitude="2"/>
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(chain.nodes()).hasSize(1);
            assertThat(chain.routes()).isEmpty();
        }

        @Test
        void mayHaveAnEmptyRoutesSection() {
            SupplyChain chain = read("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="farm" latitude="1" longitude="2"/>
                        </nodes>
                        <routes/>
                    </supplyChain>
                    """);

            assertThat(chain.routes()).isEmpty();
        }

        @Test
        void theSampleAlmondChainLoads() throws IOException {
            // Tests run with the module directory as the working directory; the sample lives at the repo root.
            Path sample = Path.of("..", "samples", "almond-chain.xml");

            SupplyChain chain;
            try (InputStream in = Files.newInputStream(sample)) {
                chain = SupplyChainXmlReader.read(in);
            }

            assertThat(chain.nodes()).hasSize(8);
            assertThat(chain.routes()).hasSize(8);
            assertThat(chain.allDownstreamOf(new NodeId("orchard-chico")))
                    .containsExactly(
                            new NodeId("huller"),
                            new NodeId("processor"),
                            new NodeId("cold-store"),
                            new NodeId("restaurant"),
                            new NodeId("port"));
        }
    }

    @Nested
    class AFileWithTheWrongShape {

        @Test
        void reportsAnUnknownNodeTypeWithItsLineNumber() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="frm" latitude="1" longitude="2"/>
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(problems).isNotEmpty().allMatch(problem -> problem.startsWith("line 3: "));
            assertThat(problems).anyMatch(problem -> problem.contains("'frm'"));
        }

        @Test
        void reportsALatitudeOutOfRange() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="farm" latitude="91" longitude="2"/>
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(problems).anyMatch(problem -> problem.startsWith("line 3: ") && problem.contains("'91'"));
        }

        @Test
        void reportsALongitudeThatIsNotANumber() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="farm" latitude="1" longitude="west"/>
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(problems).anyMatch(problem -> problem.startsWith("line 3: ") && problem.contains("'west'"));
        }

        @Test
        void reportsAMissingAttribute() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="farm" latitude="1"/>
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(problems).singleElement().asString().startsWith("line 3: ").contains("'longitude'");
        }

        @Test
        void reportsABlankName() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="  " type="farm" latitude="1" longitude="2"/>
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(problems).isNotEmpty().allMatch(problem -> problem.startsWith("line 3: "));
        }

        @Test
        void reportsAnIdContainingSpaces() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="my farm" name="Farm" type="farm" latitude="1" longitude="2"/>
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(problems).anyMatch(problem -> problem.contains("'my farm'"));
        }

        @Test
        void reportsAFileWithNoNodes() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes/>
                    </supplyChain>
                    """);

            assertThat(problems).singleElement().asString().startsWith("line 2: ");
        }

        @Test
        void reportsADifferentKindOfDocument() {
            List<String> problems = problemsIn("<shoppingList/>");

            assertThat(problems).singleElement().asString().startsWith("line 1: ").contains("shoppingList");
        }

        @Test
        void reportsProblemsOnDifferentLinesTogether() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="farm" latitude="1"/>
                            <node id="shop" name="Shop" type="customer" latitude="1" longitude="2"/>
                        </nodes>
                        <routes>
                            <route from="farm"/>
                        </routes>
                    </supplyChain>
                    """);

            assertThat(problems)
                    .hasSize(2)
                    .anyMatch(problem -> problem.startsWith("line 3: "))
                    .anyMatch(problem -> problem.startsWith("line 7: "));
        }

        @Test
        void leavesOutTheSchemaRuleCodeThatMeansNothingToAUser() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="farm" latitude="1"/>
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(problems).noneMatch(problem -> problem.contains("cvc-"));
        }
    }

    @Nested
    class AFileThatIsNotWellFormedXml {

        @Test
        void reportsWhereTheXmlBreaks() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm"
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(problems).singleElement().asString().startsWith("line 4: ");
        }

        @Test
        void reportsAnEmptyFile() {
            assertThat(problemsIn("")).singleElement().asString().startsWith("line ");
        }
    }

    @Nested
    class AFileThatIsWellShapedButMakesNoSense {

        @Test
        void reportsTheProblemsSupplyChainFinds() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="farm" latitude="1" longitude="2"/>
                            <node id="farm" name="Another Farm" type="farm" latitude="3" longitude="4"/>
                        </nodes>
                        <routes>
                            <route from="farm" to="ghost"/>
                        </routes>
                    </supplyChain>
                    """);

            assertThat(problems)
                    .containsExactly(
                            "node id 'farm' is used more than once",
                            "route farm -> ghost ends at unknown node 'ghost'");
        }

        @Test
        void reportsARouteFromANodeToItselfAlongsideOtherProblems() {
            List<String> problems = problemsIn("""
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="Farm" type="farm" latitude="1" longitude="2"/>
                        </nodes>
                        <routes>
                            <route from="farm" to="farm"/>
                            <route from="farm" to="ghost"/>
                        </routes>
                    </supplyChain>
                    """);

            assertThat(problems)
                    .containsExactly(
                            "a route cannot lead from node 'farm' back to itself",
                            "route farm -> ghost ends at unknown node 'ghost'");
        }
    }

    @Nested
    class Safety {

        @Test
        void refusesDocumentTypeDeclarationsSoAFileCannotPullInOtherFiles() {
            // The classic XML external entity attack: without protection, &secret; would be replaced
            // by the contents of a local file.
            List<String> problems = problemsIn("""
                    <?xml version="1.0"?>
                    <!DOCTYPE supplyChain [ <!ENTITY secret SYSTEM "file:///etc/passwd"> ]>
                    <supplyChain>
                        <nodes>
                            <node id="farm" name="&secret;" type="farm" latitude="1" longitude="2"/>
                        </nodes>
                    </supplyChain>
                    """);

            assertThat(problems).singleElement().asString().containsIgnoringCase("DOCTYPE");
        }

        @Test
        void passesOnAFailureToReadTheStream() {
            InputStream broken = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("disk on fire");
                }
            };

            assertThatThrownBy(() -> SupplyChainXmlReader.read(broken))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasRootCauseMessage("disk on fire");
        }

        @Test
        void rejectsANullStream() {
            assertThatNullPointerException().isThrownBy(() -> SupplyChainXmlReader.read(null));
        }
    }

    private static SupplyChain read(String xml) {
        return SupplyChainXmlReader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<String> problemsIn(String xml) {
        InvalidSupplyChainException thrown =
                catchThrowableOfType(InvalidSupplyChainException.class, () -> read(xml));
        assertThat(thrown).as("expected the file to be rejected").isNotNull();
        return thrown.problems();
    }
}
