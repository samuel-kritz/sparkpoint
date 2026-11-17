# SparkPoint: A Client-Server Tool for Mapping Wildfire and Smoke Exposure in Supply Chains

> Original project proposal, kept verbatim as the starting point. The stack and
> schedule in [PLAN.md](PLAN.md) supersede the ones proposed here.

## Project Abstract

SparkPoint will be an open-source desktop application that helps small agricultural and food-supply operators see how wildfires and wildfire smoke affect their supply chains. Users describe their chain (farms, harvest processors, warehouses, other storage endpoints, and the routes between them) in an XML file. SparkPoint retrieves close to real-time fire detections from NASA FIRMS and air-quality (AQI) observations and forecasts from the U.S. EPA's AirNow service, evaluates each location against configurable rules, and propagates the effects downstream. For example, a farm near an active fire is marked Disrupted, and the harvest processor that depends on it is flagged as Delayed. Results appear in a desktop dashboard, and every status change is stored in a local database. SparkPoint does not predict fire spread or how market rates might change. Instead, it applies user-configurable rules and shows exactly why each location received its status. There's also a wide range of end users that could subscribe to alerts. For example, a restaurant waiting on a spice shipment that would be destroyed from smoke let alone fire. Or an almond farm in Northern California waiting on truckloads of bees from Seattle to pollinate their trees. One spark point can devastate the entire supply chain.

## High Level Requirement

SparkPoint is for small operators and planners who need an early, understandable view of wildfire risk to their supply chain without buying an enterprise risk platform. From the user's point of view, the application does the following:

1. **Defines the supply chain.** The user supplies an XML file listing nodes, which might include name, type, and latitude/longitude, and if possible, the priority shipping routes between them.
2. **Collects data.** It retrieves recent fire detections from NASA FIRMS [2] and air-quality observations and forecasts at each node from AirNow [1].
3. **Evaluates each location.** Configurable rules: for example, fire detection within X kilometers, or AQI > X. Each node receives a status: Clear, Watch, Delayed, or Disrupted.
4. **Propagates downstream.** A node's status accounts for the upstream nodes it depends on, with an estimated delay and a summary of how much of the chain is affected.
5. **Presents a view and explanation.** A dashboard presents a status table, a simple map, and a detail pane explaining each status, and it alerts the user when a status changes.
6. **Logs history.** Detections, readings, and status changes are stored in a database, so the user can view a timeline for any node.
7. **Gives a short-term outlook.** It shows AirNow's air-quality forecast and a simple trend indicator: are fire detections moving closer to a node? (NO predictions!)
8. **Supports replay.** It can run from saved real data from a past fire event, so it works offline, in tests, and in demos.

## Conceptual Design

Data flow: NASA FIRMS / EPA AirNow / saved replay data → data adapters → server (supply chain graph, rule engine, database) → remote procedure calls → desktop client.

The server polls the data sources concurrently, evaluates the rules, stores the results, and notifies connected clients of changes. Each data source sits behind a common interface and each rule is a separate class. These interfaces and rules will follow design patterns similar to the PayStation, implementing Adapter and Strategy patterns, respectively. The Observer pattern can be used for status-change alerts. The supply chain and rule thresholds can be defined in XML, and the map is drawn directly rather than with a mapping library, utilizing graph search algorithms and outputting to a GUI. If we're able to complete this in 4 weeks, most of the course goals will be covered at some point: OOP, TDD, RPC, multithreading and parallel computing, drawing UML/Sequence diagrams, etc.

Proposed stack and language: Java 17 with Swing, RMI, JDBC with SQLite, JUnit 5, Maven, and Git/GitHub. (Open to other suggestions.)

We will use Java's standard library, a SQLite driver, a JSON parsing library, a unit-testing framework, and the public NASA FIRMS and AirNow data services (free API keys, 5000 requests/10 min & 500 requests/hour). We will develop everything else: the supply chain format (loading and parsing), the data adapters, the rules engine and downstream propagation, the geographic calculations, the server and remote-call interface, the database schema, the dashboard, and the tests.

## Required Resources

Access to computers with a JDK (or the chosen language's tools), a build tool, an IDE, and Git, plus free API keys from NASA FIRMS and EPA AirNow. We can use a historical sample of fire and air-quality data for replay and a sample supply chain. We need to learn geographic distance calculations, the FIRMS and AirNow data formats, remote procedure calls, database access, GUI drawing, and XML parsing.

## Background & References

Wildfire information is easy to find, but it is organized around a place or a person: where is the fire, and is the air safe to breathe. The AirNow Fire and Smoke Map [1], run by the U.S. Forest Service and EPA, shows air quality, fire locations, smoke plumes, and smoke forecast outlooks, but it is framed around personal health and does not model a user's facilities. NASA FIRMS [2] provides active-fire maps, data downloads, and email alerts from MODIS and VIIRS satellites. Watch Duty [3], a nonprofit app, adds fire alerts, perimeters, evacuation information, and air-quality readings, but it is designed for community safety, not supply chains. Commercial supplier-risk platforms such as Everstream Analytics, Resilinc, and Interos map multi-tier suppliers and monitor disruption events, but they are proprietary, enterprise-oriented products. A grower, packer, or distributor needs the supply-chain view: which of my locations are affected, and which downstream customers will feel it. An end user may benefit from the same bird's eye view.

I also reviewed the open-source Wildfire-Detection project [4], which applies machine-learning models to satellite imagery, camera images, and weather data. It addresses detection rather than supply chain impact, requires training models before it can run, and does not document automated tests. There are other wildfire spread simulation tools with a plethora of configurable knobs to adjust, but predictions are already difficult enough to make when there's a known fire. I opted to propose this project from scratch, as I did not find an open-source project that links public wildfire and smoke data to a user-defined supply chain. We won't make predictions, but we can show users the patterns as they develop in real time.

## References

1. AirNow, "About the AirNow Fire and Smoke Map." https://www.airnow.gov/fasm-v4/about
2. NASA, "Fire Information for Resource Management System (FIRMS)." https://firms.modaps.eosdis.nasa.gov
3. Sherwood Forestry Service, Inc., "Watch Duty: Wildfire Maps," App Store listing. https://apps.apple.com/app/id1574452924
4. realOmSahu, "Wildfire-Detection" (GitHub repository). https://github.com/realOmSahu/Wildfire-Detection
