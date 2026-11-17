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
Distance calculations are added in Milestone 1.
