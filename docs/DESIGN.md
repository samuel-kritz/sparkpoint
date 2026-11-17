# SparkPoint Design

This document describes SparkPoint as built. It grows one section per
milestone; the roadmap and the reasoning behind the stack are in
[PLAN.md](PLAN.md).

## 1. Architecture overview

SparkPoint is two programs. The **server** collects fire and air-quality data,
evaluates the supply chain, and keeps the history. The **client** is a desktop
dashboard that asks the server for the current picture and listens for changes.

```mermaid
flowchart LR
    subgraph Sources
        FIRMS[NASA FIRMS<br/>fire detections]
        AirNow[EPA AirNow<br/>AQI observations + forecast]
        Replay[(Replay dataset<br/>recorded real event)]
    end

    subgraph Server
        Adapters[Data adapters]
        Poller[Poll scheduler]
        Engine[Rule engine +<br/>downstream propagation]
        Store[(SQLite history)]
        Publisher[Status-change publisher]
        Rpc[gRPC service]
    end

    subgraph Client
        Dash[Swing dashboard]
    end

    Chain[/supply-chain.xml<br/>rules.xml/] --> Engine
    FIRMS --> Adapters
    AirNow --> Adapters
    Replay --> Adapters
    Adapters --> Poller --> Engine
    Engine --> Store
    Engine --> Publisher --> Rpc
    Store --> Rpc
    Rpc <-->|unary calls + status stream| Dash
```

### Modules and their dependencies

```mermaid
flowchart TD
    client[sparkpoint-client<br/>Swing dashboard]
    server[sparkpoint-server<br/>adapters, polling, persistence, RPC service]
    proto[sparkpoint-proto<br/>RPC contract]
    core[sparkpoint-core<br/>domain, geo, rules, propagation]

    client --> proto
    server --> proto
    server --> core

    classDef planned stroke-dasharray: 5 5
    class client,server,proto planned
```

Dashed modules are planned and do not exist yet. Two rules hold the structure
together:

- **`sparkpoint-core` is pure.** It has no compile-scope dependencies and does
  no network, database, file-watching or UI work. Everything in it can be
  tested with plain objects.
- **The client knows only the contract.** It never depends on core or server
  classes, so everything it shows has crossed the RPC boundary.

### Cross-cutting decisions

| Decision | Reason |
|---|---|
| All time comes from an injected `java.time.Clock`. | Replay runs a past event on a simulated clock, and tests are deterministic. |
| Data sources sit behind interfaces with live and replay implementations. | The server cannot tell a recorded fire from a live one, so the demo exercises the real code path. |
| Missing data is an explicit state, never treated as "clear". | A node with no nearby air monitor must not look safe. |
| Compiler warnings fail the build. | Problems are fixed when introduced, not accumulated. |

### Build

Maven multi-module, run through the Maven Wrapper (`./mvnw verify`). The parent
POM pins every dependency and plugin version, requires Java 21 or newer, and
produces a JaCoCo coverage report per module. `sparkpoint-core` additionally
fails the build below 90% line or 85% branch coverage. GitHub Actions runs the
same command on every pull request.

## 2. Geographic model

`GeoPoint` (in `sparkpoint-core`, package `geo`) is a latitude/longitude pair in
decimal degrees on WGS 84, the datum both data sources use. It validates its
range on construction, so no other code needs to check coordinates again.

`GreatCircle` holds the two distance calculations the rules need. Both return
kilometres and treat the Earth as a sphere of mean radius 6371.0088 km.

| Method | Answers | Used by |
|---|---|---|
| `distanceKm(from, to)` | How far apart are two places? | Fire-proximity rule, trend indicator |
| `distanceToSegmentKm(point, start, end)` | How close does a fire come to any part of a route? | Route fire-proximity rule |

```mermaid
flowchart LR
    A((start)) --- B((end))
    P1[fire beside the route] -. perpendicular distance .-> M[nearest place on the route]
    P2[fire beyond the end] -. distance to the end .-> B
    P3[fire behind the start] -. distance to the start .-> A
```

Decisions and their limits:

- **Sphere, not ellipsoid.** The error is at most about 0.5%, far smaller than
  the uncertainty in a satellite fire detection (hundreds of metres) relative
  to thresholds measured in whole kilometres.
- **A route is the straight great-circle line between two nodes**, not the road
  a truck drives. A fire near a winding road but far from the straight line is
  not caught. Routes with intermediate waypoints would fix this and can be
  added to the chain format later without changing this calculation.
- **Routes are assumed shorter than a quarter of the Earth's circumference**
  (about 10,000 km), which the point-to-route formula relies on.
- **Distances are plain `double` kilometres** with a `Km` suffix on method
  names, not a wrapper type. Kilometres are the only unit in the system, so a
  type would add ceremony without preventing a real mistake.

## 3. Supply-chain graph

Package `chain` in `sparkpoint-core` models the chain as a directed graph
without cycles. "Upstream" means towards the origin of the goods, "downstream"
towards the customer.

```mermaid
classDiagram
    class SupplyChain {
        +of(nodes, routes)$ SupplyChain
        +nodes() List~Node~
        +routes() List~Route~
        +node(NodeId) Optional~Node~
        +upstreamOf(NodeId) List~NodeId~
        +downstreamOf(NodeId) List~NodeId~
        +allDownstreamOf(NodeId) List~NodeId~
        +upstreamFirst() List~Node~
    }
    class Node {
        <<record>>
        NodeId id
        String name
        NodeType type
        GeoPoint location
    }
    class Route {
        <<record>>
        NodeId from
        NodeId to
    }
    class NodeId {
        <<record>>
        String value
    }
    class NodeType {
        <<enumeration>>
        SUPPLIER
        FARM
        PROCESSOR
        WAREHOUSE
        PORT
        CUSTOMER
        OTHER
    }
    class InvalidSupplyChainException {
        +problems() List~String~
    }
    SupplyChain "1" o-- "1..*" Node
    SupplyChain "1" o-- "*" Route
    Node --> NodeId
    Node --> NodeType
    Node --> GeoPoint
    Route --> NodeId : from, to
    SupplyChain ..> InvalidSupplyChainException : throws
```

`SupplyChain.of` is the only way to create a chain, and it validates
everything, so any `SupplyChain` that exists is usable. It rejects:

| Problem | Example message |
|---|---|
| No nodes | `a supply chain needs at least one node` |
| Repeated node id | `node id 'farm' is used more than once` |
| Route to or from a missing node | `route farm -> ghost ends at unknown node 'ghost'` |
| Repeated route | `route farm -> shop is listed more than once` |
| Cycle | `routes form a cycle, so nothing in it comes first: a -> b -> c -> a` |

Decisions:

- **All problems are reported together.** A user fixing a hand-written file
  should not have to run it once per mistake.
- **Cycles are rejected.** A disruption propagates downstream; in a cycle every
  node is downstream of itself and there is no first node to evaluate.
- **`upstreamFirst()` is a topological order** (Kahn's algorithm), computed
  once at construction. The propagation step in Milestone 2 walks it so that a
  node's upstream statuses are known before its own. Ties are broken by the
  order nodes appear in the file, so the result is deterministic.
- **`NodeType` is descriptive only.** No rule behaves differently by type.
- **A `Route` carries only its two ends for now.** Transit time and priority
  are added when the delay estimate in Milestone 2 needs them.
- **The chain is immutable.** It is shared between the polling threads and the
  RPC threads later, and an immutable object needs no locking.

## 4. Supply-chain file format

A chain is described in XML. The format is defined by `supply-chain.xsd`, which
ships inside `sparkpoint-core`, and read by `SupplyChainXmlReader`. A complete
example is [samples/almond-chain.xml](../samples/almond-chain.xml).

```xml
<supplyChain>
    <nodes>
        <node id="orchard" name="Chico Almond Orchard" type="farm" latitude="39.7285" longitude="-121.8375"/>
        <node id="huller" name="Orland Huller" type="processor" latitude="39.7474" longitude="-122.1964"/>
    </nodes>
    <routes>
        <route from="orchard" to="huller"/>
    </routes>
</supplyChain>
```

| Attribute | Allowed values |
|---|---|
| `node/@id` | Letters, digits, `.`, `_`, `-` |
| `node/@name` | Any text that is not blank |
| `node/@type` | `supplier`, `farm`, `processor`, `warehouse`, `port`, `customer`, `other` |
| `node/@latitude` | Decimal degrees, -90 to 90, north positive |
| `node/@longitude` | Decimal degrees, -180 to 180, east positive |
| `route/@from`, `route/@to` | The id of a node; goods flow from `from` to `to` |

The `routes` section is optional.

### Two-stage checking

```mermaid
flowchart LR
    File[/chain.xml/] --> Parse[Parse + schema check]
    Parse -->|shape is wrong| E1[InvalidSupplyChainException<br/>problems with line numbers]
    Parse -->|shape is right| Build[SupplyChain.of]
    Build -->|chain makes no sense| E2[InvalidSupplyChainException<br/>problems in plain words]
    Build -->|ok| Chain([SupplyChain])
```

- **The schema checks shape**: elements, attributes, value ranges. These
  problems come with line numbers.
- **`SupplyChain.of` checks meaning**: unique ids, routes that refer to real
  nodes, no cycles. XSD could express the first two, but its messages for them
  are far harder to read than the ones `SupplyChain` writes.
- A file with shape problems is not checked for meaning, because a chain cannot
  be built from it.

Decisions:

- **The reader takes an `InputStream`, not a file path.** Opening files is left
  to the caller, which keeps `sparkpoint-core` free of file handling and makes
  the reader trivially testable from strings.
- **DOCTYPE declarations are refused.** A chain file may come from someone
  else, and this closes off XML external-entity and entity-expansion attacks.
- **No XML namespace.** The file is written by hand; a namespace would add a
  line of boilerplate that is easy to get wrong.
- **Known roughness:** schema messages come from the JDK's XML parser. They are
  accurate but wordy, and one mistake usually produces two lines (the reason,
  then the attribute it applies to).
