# Dynamic Directory Service with Homomorphic Encryption (FaaS Marketplace Branch)

This branch introduces the **FaaS Marketplace** extension on top of the DNS++ dynamic directory service substrate. It builds upon and the core homomorphic encryption (Paillier) and pub/sub mechanisms to build a decentralized, privacy-preserving orchestration of serverless functions across the Edge-Cloud Continuum.

## 1. System Requirements & Build

The underlying cryptographic and simulation substrate requires a version of the Java language **equal to or greater than 23**. If you have multiple versions of Java installed, configure `JAVA_HOME` before compiling.

### macOS
Set `JAVA_HOME` to use Java 23:
```console
$ export JAVA_HOME=$(/usr/libexec/java_home -v 23)
$ java -version
```

### Linux
If Java 23 is installed but not the default version, manually specify the path:
```console
$export JAVA_HOME=/path/to/java23$ export PATH=$JAVA_HOME/bin:$PATH
$ java -version
```

### Build & Clean
Change the current directory to the one containing the project's source and run the Ant build target:
```console
$ ant jar
```
This compiles the source code and generates the JAR file in the `build/jar` directory. 

To remove all compiled files and generated JARs, or to perform a clean rebuild:
```console
$ ant clean
$ ant clean-build
```

### Basic Execution
By default, the generated JAR file will run the `experiments.Main` class:
```console
$ java -jar build/jar/DNSPlus.jar
```
To run a specific simulation or marketplace batch, specify the JAR as part of the classpath:
```console
$ java -cp build/jar/DNSPlus.jar <fully.qualified.ClassName>
```

---

## 2. Privacy-Preserving FaaS Marketplace Simulations

This section outlines the execution of the FaaS Marketplace extension, designed to evaluate multi-objective function placement across the Edge-Cloud Continuum. The marketplace relies on distributed `AbstractMarketplaceBroker` agents performing two-phase request orchestration: **State Aggregation** (building the `OfferIndex` using multi-dimensional `Capability Hyperrectangles`) and **Feasibility Checking / Cost Minimization** (routing requests via weighted sum scalarization). 

To preserve client intent, the substrate utilizes Paillier homomorphic encryption for blind function identifier resolution during the topological traversal.

### Configuration (`simulation.properties`)

The marketplace simulation uses the standard `simulation.properties` file to define the topology, provider distribution, workload skew, and routing strategies. Below is the configuration subset critical for reproducing marketplace behavior.

#### Topology Generation and Slicing
The physical substrate dictates the latency boundaries of the `Capability Hyperrectangles`. We utilize the `POLITICAL` strategy to generate realistic edge/fog/cloud latency models based on global demographic data.

```properties
# Sets the underlying graph builder strategy. POLITICAL enforces hierarchical admin boundaries.
topology.generation.strategy=POLITICAL
paths.useFullTopology=true

# Slices the global GeoNames DB to a specific subset to bound the O(log N) homomorphic traversal overhead.
marketplace.topology.slice=United States
```

#### Provider Distribution (Edge-Cloud Continuum)
This configures the baseline population of `MarketplaceProvider` instances. The hierarchy injects heterogeneity into the $I^d$ (QoS Coverage) capabilities available at different topological depths.

```properties
# Defines the multi-tier capability injection. 
marketplace.providers.cloud.count=25
marketplace.providers.fog.count=250
marketplace.providers.edge.count=2500
```

#### Marketplace Orchestration Strategy
These parameters strictly control the optimization boundaries and how `AbstractMarketplaceBroker` manages the `OfferIndex`.

```properties
# Controls how Phase 1 merges Capability Hyperrectangles.
# WEIGHTED reduces "False Positive Dispatch" rates by factoring in offer density when computing centroids (c^d).
# UNWEIGHTED prioritizes maximal coverage but risks higher optimality gaps deep in the tree.
marketplace.aggregation.strategy=WEIGHTED

# Defines the Phase 2 multi-objective placement logic.
# WEIGHTED_UTILITY: Evaluates spatial, latency, and cost vectors via scalarization.
# BASELINE: Forces requests to a centralized provider, simulating legacy FaaS gateways.
marketplace.routing.strategy=WEIGHTED_UTILITY
marketplace.baseline.vendor=AWS_Cloud
```

#### Workload Constraints
These parameters tweak the constraint vectors of the injected `ServiceRequest` objects, stressing the Feasibility Checking phase.

```properties
# Probability that a request demands ultra-low latency, forcing a localized Edge execution.
marketplace.workload.edge.probability=0.0

# Probability that a request has strict financial constraints, risking Proactive Drops if no feasible hyperrectangle exists.
marketplace.workload.strict_budget.probability=0.0
```

### Executing Marketplace Simulations

The `marketplace` package contains dedicated entry points for running deterministic scenarios. Ensure the project is compiled (`ant clean-build`) before executing these classes.

#### Running a Single Continuum Simulation
To debug specific scalarization logic, step through the `AbstractMarketplaceBroker` matching execution, or simply run a baseline test, you can execute a single, monolithic simulation. This run is governed strictly by the static parameters currently set in your `simulation.properties` file.

To run this single scenario, execute the class containing the `main` method for standalone runs:
```console
$ java -cp build/jar/DNSPlus.jar marketplace.simulations.SystematicMarketplaceContinuumSimulation
```

**Note on Event Tracing:** If you are debugging routing algorithms, ensure `paths.enableEventTracing=true` is set in your properties file. This utilizes the `MarketplaceMetricsCollector` to output a detailed CSV containing the exact multi-dimensional capability vectors calculated at each broker hop. *Warning: Disable this for large batch runs to prevent severe I/O bottlenecking and memory heap exhaustion.*

#### Running Systematic Scenario Batches
To evaluate system performance tradeoffs (e.g., state compression vs. placement accuracy) across varying constraints, you must run batch simulations. Instead of manually editing the `simulation.properties` file for every run, the framework provides a scenario hierarchy to automate parameter sweeps.

**Class Hierarchy & Configuration:**
The batch orchestration is driven by a class hierarchy rooted in `AbstractMarketplaceScenario`. Specific experiment parameters are defined in its concrete subclasses:
* `EdgeProbabilityScenario`: Sweeps varying values for latency-constrained request probabilities.
* `BudgetProbabilityScenario`: Sweeps varying values for strict budget constraints.
* `AggregationThresholdScenario`: Sweeps the allowable aggregation error ($\epsilon_{agg}$) to benchmark memory footprint against placement optimality.

*Crucially, these scenario classes do not contain a `main` method.* They act as configuration templates. To adjust the parameter sweep (e.g., changing the tested probabilities from `[0.0, 0.2, 0.4]` to `[0.1, 0.5, 0.9]`), you must open the respective scenario class file (e.g., `BudgetProbabilityScenario.java`) and modify the sweep arrays defined within its setup logic, then recompile the project.

**Executing the Batch:**
The actual execution and aggregation of metrics (SLA violations, False Positives, Optimality Gaps) are handled by the `MarketplaceBatchRunner` (or orchestrator class), which contains the `main` method. 

Before running, ensure the `MarketplaceBatchRunner` is configured to instantiate the specific scenario subclass you wish to evaluate. Then, execute the batch runner:

```console
$ java -cp build/jar/DNSPlus.jar marketplace.simulations.MarketplaceBatchRunner
```

---

## 3. Base DNS++ Framework & Substrate Simulator

*(The following sections detail the underlying DNS++ test harness and geographic routing simulator utilized by the marketplace substrate).*

### Defining Legacy Experiments
The codebase provides a framework to define and execute various experiments using different configurations. Each experiment includes a Broker and a number of *Publishers* generating *notifications* about services and *Subscribers* interested in *subscribing* and receiving those *notifications*. The encryption mechanism is based on a pre-existing modified implementation of Paillier with a key length of `n=2048` bits.

#### Entities
* **Publisher**: Represents an entity that generates and publishes content.
* **ReceivingSubscriber**: Represents an entity subscribing to content and receiving publications.
* **Broker**: Currently, the primary substrate broker is `AsynchronousBrokerWithBinaryBalancedTreeAndCache`. This broker utilizes multiple threads consuming publications and subscriptions from separate queues, plus a dispatcher delivering publications to the right subscribers according to the matching results. It provides a cache for publications to immediately return results to subscribers if a match exists.

#### Task Execution & Generation
Operations within a task can be specified explicitly (submitting a list of domain names) or randomly (using a probability distribution based on domain popularity specified in `ranked_websites.csv`). The `TaskGenerator` class provides methods to add tasks for different phases (`preTask`, `task`, `postTask`).

See `experiments.Main` for examples of initializing `DNSWithCacheAsynchronousSequentialParallelExperiment` and `DNSWithCacheAsynchronousExperiment`.

### The Substrate Simulator
The publish-subscribe simulator provides an orthogonal set of experiments, focusing on modelling and evaluating different geographical routing algorithms to test localization and message propagation on a large scale.

* **Region-Based Routing**: Subscribers declare interest in a specific geographical area. Publications falling within this region are routed to the subscriber (one-to-many delivery).
* **Location-Based Routing**: Subscribers express interest in receiving information *closest* to their own location. The broker network finds the most relevant publication (best-effort delivery).

#### Executing Geographical Simulations
Running a simulation involves Topology Generation (`TopologyFactory`), Client Attachment (`TopologyPopulator`), and Scenario Execution (`SimulationRunner`). Topologies include FIXED, GRID, RANDOM, and file-based GEONAMES.

To run a performance simulation, execute the relevant suite main class:
```console
$ java -cp build/jar/DNSPlus.jar simulator.RegionPerformanceSimulationsMain
```

For functional debugging, you can enable the `TopologyVisualiser` (using the `GraphStream` library) by utilizing the `VisualisedSimulationRunner` to trace the step-by-step flow of subscriptions and publications across the broker tree.