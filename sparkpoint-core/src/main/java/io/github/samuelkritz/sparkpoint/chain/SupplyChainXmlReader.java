package io.github.samuelkritz.sparkpoint.chain;

import io.github.samuelkritz.sparkpoint.geo.GeoPoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Reads a supply chain from the XML format defined by {@code supply-chain.xsd}.
 *
 * <p>Checking happens in two stages. The schema checks the shape of the file and reports problems
 * with line numbers. If the shape is right, {@link SupplyChain#of} checks that the chain makes
 * sense as a whole. Either way the caller gets one {@link InvalidSupplyChainException} listing
 * every problem found at that stage.
 */
public final class SupplyChainXmlReader {

    private static final String SCHEMA_RESOURCE = "supply-chain.xsd";

    /** Compiled once; a {@link Schema} is immutable and safe to share between threads. */
    private static final Schema SCHEMA = loadSchema();

    private SupplyChainXmlReader() {}

    /**
     * Reads a supply chain. The caller opens and closes the stream.
     *
     * @throws InvalidSupplyChainException if the content is not well-formed XML, does not match the
     *     schema, or does not describe a usable chain
     * @throws UncheckedIOException if the stream cannot be read
     */
    public static SupplyChain read(InputStream xml) {
        Objects.requireNonNull(xml, "xml");
        Document document = parse(xml);

        List<Node> nodes = new ArrayList<>();
        NodeList nodeElements = document.getElementsByTagName("node");
        for (int i = 0; i < nodeElements.getLength(); i++) {
            nodes.add(toNode((Element) nodeElements.item(i)));
        }

        List<String> problems = new ArrayList<>();
        List<Route> routes = new ArrayList<>();
        NodeList routeElements = document.getElementsByTagName("route");
        for (int i = 0; i < routeElements.getLength(); i++) {
            Element element = (Element) routeElements.item(i);
            try {
                routes.add(new Route(new NodeId(element.getAttribute("from")), new NodeId(element.getAttribute("to"))));
            } catch (IllegalArgumentException routeToItself) {
                // The one rule Route enforces itself. Record it and carry on, so that it is reported
                // together with whatever SupplyChain finds rather than instead of it.
                problems.add(routeToItself.getMessage());
            }
        }

        try {
            SupplyChain chain = SupplyChain.of(nodes, routes);
            if (problems.isEmpty()) {
                return chain;
            }
        } catch (InvalidSupplyChainException invalid) {
            problems.addAll(invalid.problems());
        }
        throw new InvalidSupplyChainException(problems);
    }

    /** Parses and schema-validates, so that everything read from the returned document is known to be present and well-typed. */
    private static Document parse(InputStream xml) {
        ProblemCollector collector = new ProblemCollector();
        Document document;
        try {
            DocumentBuilder builder = newSecureFactory().newDocumentBuilder();
            builder.setErrorHandler(collector);
            document = builder.parse(xml);
        } catch (SAXParseException notWellFormed) {
            // A fatal error: the parser cannot continue, so this is the only problem there is to report.
            throw new InvalidSupplyChainException(List.of(describe(notWellFormed)));
        } catch (SAXException | ParserConfigurationException e) {
            throw new IllegalStateException("could not set up the XML parser", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!collector.problems.isEmpty()) {
            throw new InvalidSupplyChainException(collector.problems);
        }
        return document;
    }

    private static DocumentBuilderFactory newSecureFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setSchema(SCHEMA);
        factory.setNamespaceAware(true);
        // A supply-chain file may come from someone else. Refusing DOCTYPE declarations outright
        // closes off external-entity and entity-expansion attacks, and the format has no use for them.
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static Node toNode(Element element) {
        return new Node(
                new NodeId(element.getAttribute("id")),
                element.getAttribute("name"),
                NodeType.valueOf(element.getAttribute("type").toUpperCase(Locale.ROOT)),
                new GeoPoint(
                        Double.parseDouble(element.getAttribute("latitude")),
                        Double.parseDouble(element.getAttribute("longitude"))));
    }

    private static Schema loadSchema() {
        URL schema = SupplyChainXmlReader.class.getResource(SCHEMA_RESOURCE);
        if (schema == null) {
            throw new IllegalStateException("built-in schema " + SCHEMA_RESOURCE + " is missing from the classpath");
        }
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return factory.newSchema(schema);
        } catch (SAXException e) {
            throw new IllegalStateException("built-in schema " + SCHEMA_RESOURCE + " is not valid", e);
        }
    }

    private static String describe(SAXParseException problem) {
        // Schema messages start with a rule code such as "cvc-complex-type.4: ", which means nothing to a user.
        String message = problem.getMessage().replaceFirst("^cvc-[^:]+: ", "");
        return "line " + problem.getLineNumber() + ": " + message;
    }

    /** Gathers schema violations instead of stopping at the first one. */
    private static final class ProblemCollector implements ErrorHandler {

        private final List<String> problems = new ArrayList<>();

        @Override
        public void warning(SAXParseException warning) {
            problems.add(describe(warning));
        }

        @Override
        public void error(SAXParseException error) {
            problems.add(describe(error));
        }

        @Override
        public void fatalError(SAXParseException fatal) throws SAXParseException {
            // Not recoverable: rethrow so parse() reports it once, on its own.
            throw fatal;
        }
    }
}
