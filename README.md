# SparkPoint

A client-server desktop tool that maps wildfire and smoke exposure onto a
user-defined supply chain.

You describe your chain (farms, processors, warehouses and the routes between
them) in an XML file. SparkPoint pulls near-real-time fire detections from
[NASA FIRMS](https://firms.modaps.eosdis.nasa.gov) and air-quality observations
and forecasts from [EPA AirNow](https://www.airnow.gov), evaluates each location
against configurable rules, and propagates the effects downstream. Every status
comes with the reasons behind it. SparkPoint does not predict fire spread.

**Status: early development.** The build and the first domain type exist;
nothing is runnable yet.

- [Project plan](docs/PLAN.md): stack, milestones, how the work is organised
- [Design](docs/DESIGN.md): architecture and diagrams, updated as the code grows
- [Original proposal](docs/PROPOSAL.md)
- [Working with Claude Code](docs/WORKING-WITH-CLAUDE.md): how this project is built with an AI assistant, and how its work is reviewed

## Building

You need a JDK, version 21 or newer. Maven is downloaded by the wrapper.

```bash
./mvnw verify
```

This compiles everything, runs the tests, and writes a coverage report to
`sparkpoint-core/target/site/jacoco/index.html`.

In IntelliJ IDEA, open the project folder and it will import the Maven modules
from `pom.xml`.

## Licence

[MIT](LICENSE)
