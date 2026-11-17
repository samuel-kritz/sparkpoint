# SparkPoint

A client-server desktop tool that maps wildfire and smoke exposure onto a
user-defined supply chain.

You describe your chain (farms, processors, warehouses and the routes between
them) in an XML file. SparkPoint pulls near-real-time fire detections from
[NASA FIRMS](https://firms.modaps.eosdis.nasa.gov) and air-quality observations
and forecasts from [EPA AirNow](https://www.airnow.gov), evaluates each location
against configurable rules, and propagates the effects downstream. Every status
comes with the reasons behind it. SparkPoint does not predict fire spread.

**Status: early development.** Nothing is runnable yet.

- [Project plan](docs/PLAN.md): stack, architecture, milestones
- [Original proposal](docs/PROPOSAL.md)

## Licence

[MIT](LICENSE)
