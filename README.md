# Dynamic Directory Service with Homomorphic Encryption Based Privacy

This project aims to create a Directory Service that addresses dynamic information distribution via a *pub/sub* mechanism and provides *privacy* to users interacting with the system using *homomorphic encryption*.

---

## Requirements

The project requires a version of the Java language **equal to or greater than 23**.

If you have multiple versions of Java installed, please make sure to configure `JAVA_HOME` properly before compiling the project.

### macOS

On macOS, you can set `JAVA_HOME` to use Java 23 by running:

```console
$ export JAVA_HOME=$(/usr/libexec/java_home -v 23)
````

To verify that Java 23 is being used, run:

```console
$ java -version
```

### Linux

On Linux, make sure `JAVA_HOME` is set correctly. If Java 23 is installed but not the default version, you can manually specify it:

```console
$export JAVA_HOME=/path/to/java23$ export PATH=$JAVA_HOME/bin:$PATH
```

Replace `/path/to/java23` with the actual path where Java 23 is installed.

To check the active Java version, run:

```console
$ java -version
```

## Build

Change the current directory to the one containing the project's source and run the command:

```console
$ ant jar
```

This will **compile the source code** and generate the JAR file in the `build/jar` directory.

## Run the Project

By default, the generated JAR file will run the `experiments.Main` class. You can execute it using:

```console
$ java -jar build/jar/DNSPlus.jar
```

### Run Other Classes

If you want to run a different class from the JAR (e.g., `simulator.Main`), you need to specify the JAR as part of the **classpath** using the `-cp` flag:

```console
$ java -cp build/jar/DNSPlus.jar simulator.Main
```

Make sure that the class you want to run has a valid `public static void main(String[] args)` method.

## Clean the Project

To remove all compiled files and generated JARs, run:

```console
$ ant clean
```

This will delete the `build/` directory and all its contents.

To **clean and rebuild** the project in one step, run:

```console
$ ant clean-build
```

## Defining Experiments

The current code provides a framework to define and execute various experiments using different configurations. Each experiment includes a Broker and a number of *Publishers* generating *notifications* about services and *Subscribers* interested in *subscribing* and receiving those *notifications*. The encryption mechanism is based on a pre-existing modified implementation of Paillier with a key length of `n=2048` bits.

Some placeholder classes are available in the codebase and can be used as templates for writing customised experiments. Please refer to the examples provided below.

### Entities

  - **Publisher**: Represents an entity that generates and publishes content.
  - **ReceivingSubscriber**: Represents an entity subscribing to content and receiving publications.
  - **Broker**: Different types of brokers are available, but currently, the one to be used is the `AsynchronousBrokerWithBinaryBalancedTreeAndCache` class. This broker is based on multiple threads consuming publications and subscriptions from separate queues, plus a dispatcher delivering publications to the right subscribers according to the matching results. This implementation also provides a cache for publications the broker receives that are added to the cache if seen for the first time. When a subscription arrives, the content of the cache is checked for matching publications, and a result is immediately returned to a subscriber if possible.

### Three-phase Parallel Task Execution with Notification Caching

This experiment allows for the definition and running of experiments with various entities such as publishers and subscribers. Each entity can participate in tasks during three phases: `preTask`, `task`, and `postTask`. Operations in each phase are executed in parallel.

#### Task Phases

1.  **preTask**: Tasks executed before the main experiment run.
2.  **task**: Main tasks executed during the experiment run.
3.  **postTask**: Tasks executed after the main experiment run.

#### Task Operations and Generation

Operations within a task can be specified in two ways:

1.  **Explicitly**: By submitting a list of domain names to be used for either subscriptions or publications.
2.  **Randomly**: When the random option is selected, a probability distribution is used based on the list of domains and their frequency (popularity) specified in the input file (an example is provided in `ranked_websites.csv`).

The `TaskGenerator` class is responsible for generating tasks for the experiment. It provides methods to add tasks for the different phases:

  - `addRandomisedPreTask(BlindingEntity entity, int numberOfOperations)`: Adds a randomised preTask.
  - `addRandomisedTask(BlindingEntity entity, int numberOfOperations)`: Adds a randomised main task.
  - `addRandomisedPostTask(BlindingEntity entity, int numberOfOperations)`: Adds a randomised postTask.
  - `addTask(BlindingEntity entity, List<String> domains)`: Adds a task with specific domains.
  - `addRunCleaningTasks()`: Adds cleaning tasks to ensure all necessary tasks are executed.

#### Example Usage

Here's an example of how to define and run an experiment with the above properties using the `DNSWithCacheAsynchronousSequentialParallelExperiment` class:

```java
public void executeRun() {
    TaskGenerator taskGenerator = new TaskGenerator();
    experimentRun = new DNSWithCacheAsynchronousSequentialParallelRun();
    experimentRun.setUp();

    Publisher pub1 = new Publisher("pub1");
    Publisher pub2 = new Publisher("pub2");
    ReceivingSubscriber sub1 = new ReceivingSubscriber("sub1");
    ReceivingSubscriber sub2 = new ReceivingSubscriber("sub2");

    taskGenerator.addRandomisedPreTask(pub1, 500);
    taskGenerator.addRandomisedTask(sub1, 10);
    taskGenerator.addTask(new ReceivingSubscriber("sub3"), new ArrayList<>(Arrays.asList("[www.facebook.com](https://www.facebook.com)")));
    taskGenerator.addRandomisedTask(pub1, 100);
    taskGenerator.addRandomisedTask(pub2, 200);
    taskGenerator.addRandomisedPostTask(sub1, 20);
    taskGenerator.addRunCleaningTasks();

    experimentRun.executeRun();
    experimentRun.finalise();

    List<RunTasksOutputManager> allRunsTasksOutput = getAllRunsTasksOutput();
    allRunsTasksOutput.add(experimentRun);
}
```

This example demonstrates how to set up publishers and subscribers, add tasks for different phases, and execute the experiment run. Each experiment is based on executing the operations defined in a corresponding run containing the task definition. At the end of an experiment, the average execution time of operations is provided as output.

```
[experiments.outputdata.BrokerStatsCollector][asynchronousMeasurementPerformed]: BrokerStats{numberOfPublications=804, numberOfSubscriptions=34, numberOfMatches=24, numberOfCacheHits=22}
{name=DNSWithCacheAsynchronousSequentialParallelExperiment-1736523196080, inputFileName=ranked_websites.csv, numberOfRuns=2,
    experimentOutput={name=DNSWithCacheAsynchronousSequentialParallelExperiment, tasksStats=[
        {PRE_TASK, name=pub1:publish:500, averageDuration=6018.0, durationStandardDeviation=141.0, averageReplyDuration=318.5, replyDurationStandardDeviation=56.5},
        {TASK, name=sub1:subscribe:10, averageDuration=263.0, durationStandardDeviation=40.0, averageReplyDuration=16.0, replyDurationStandardDeviation=3.0},
        {TASK, name=subbb:subscribe:1, averageDuration=30.5, durationStandardDeviation=7.5, averageReplyDuration=16.0, replyDurationStandardDeviation=3.0},
        {TASK, name=pub1:publish:100, averageDuration=939.0, durationStandardDeviation=28.0, averageReplyDuration=285.0, replyDurationStandardDeviation=13.0},
        {TASK, name=pub2:publish:200, averageDuration=3018.0, durationStandardDeviation=22.0, averageReplyDuration=285.0, replyDurationStandardDeviation=13.0},
        {POST_TASK, name=sub1:subscribe:20, averageDuration=277.0, durationStandardDeviation=29.0, averageReplyDuration=22.0, replyDurationStandardDeviation=1.0},
        {POST_TASK, name=pub:nop, averageDuration=0.0, durationStandardDeviation=0.0, averageReplyDuration=0.0, replyDurationStandardDeviation=0.0}]}}
```

### Single-phase Parallel Task Execution with Notification Caching

In this simpler experiment, we use the `DNSWithCacheAsynchronousExperiment` class to set up and run the experiment. This class does not provide a three-phase task execution but focuses on adding publishers and subscribers and running their tasks.

The constructor initialises the experiment with the input file name, number of runs, publications, and subscriptions. It also sets the number of publishers and subscribers.

The `addPublishers` method creates and adds `PublisherTask` instances to the experiment run. Each task is configured with the input file name and the specified number of publications.

The `addSubscribers` method creates and adds `SubscriberTask` instances to the experiment run. Each task is configured with the input file name and the specified number of subscriptions.

The above methods generate publications and subscriptions randomly based on the probability calculated from the input file containing the domain names and associated frequencies.

The `executeRun` method sets up the experiment run, adds the publishers and subscribers, executes it, and finalises it. The results of all runs are collected and stored.

```java
@Override
protected void executeRun() {
    experimentRun = new DNSWithCacheAsynchronousRun();
    experimentRun.setUp();

    addPublishers();
    addSubscribers();

    experimentRun.executeRun();

    experimentRun.finalise();

    List<RunTasksOutputManager> allRunsTasksOutput = getAllRunsTasksOutput();
    allRunsTasksOutput.add(experimentRun);
}
```

### Main Class

A `Main` class is provided as an example in the current code. It demonstrates how to initialise and start an experiment:

```java
public class Main {

    private static final Logger logger = CustomLogger.getLogger(Experiment.class.getName(), Level.INFO);

    public static void main(String[] args) {
        Experiment experiment1 = new DNSWithCacheAsynchronousSequentialParallelExperiment("ranked_websites.csv", 10, 1, 3);
        experiment1.start();

        Experiment experiment2 = new DNSWithCacheAsynchronousExperiment("ranked_websites.csv", 10, 3, 3);
        experiment2.start();
    }
}
```

This `Main` class shows how to configure and run the two types of experiments using the provided classes.


## The Simulator

This section provides a detailed overview of the publish-subscribe simulator. Its purpose is to perform an orthogonal set of experiments, separate from the cryptographic performance tests of the main system. The simulator focuses on modelling and evaluating different geographical routing algorithms to test localisation and message propagation on a large scale. We will explore its architecture, routing mechanisms, simulation components, and extensibility.

### Simulator and Routing Algorithms: An Overview

The simulator is a Java-based application that models a publish-subscribe system where subscribers express interest in certain types of information, and publishers provide that information. The core of the simulator is a hierarchical network of brokers that route publications from publishers to the appropriate subscribers. The simulator is designed to evaluate two primary, geographically-based routing algorithms:

  * **Region-Based Routing**: In this model, subscribers declare their interest in a specific geographical area or *region*. Any publication with a location that falls within this region is routed to the subscriber. This is a one-to-many delivery system, where a single publication can be delivered to many subscribers.

  * **Location-Based Routing**: This algorithm is more specific. Subscribers express interest in receiving information that is *closest* to their own location. The broker network is responsible for finding the most relevant (i.e., nearest) publication for each subscriber, making it a "best-effort" delivery system rather than a broadcast to all interested parties in a region.

These two approaches have different performance characteristics and are suited for different types of applications. The simulator is designed to allow for a detailed comparison of their efficiency and scalability.

### Simulator Architecture

The simulator is organised into several packages, each with a distinct responsibility, creating a modular and extensible framework.

  * **`simulator.core`**: This package forms the foundation of the simulator. It contains the essential classes that define the basic structure and execution flow of a simulation.
  * **`simulator.entities`**: This package defines the active participants in the publish-subscribe system: `SimulationBroker`, `SubscriberWithLocation`, and `PublisherWithLocation`.
  * **`simulator.events`**: This package contains the classes that represent the messages exchanged between entities, such as `SimulationSubscription` and `SimulationPublication`.
  * **`simulator.regions`**: This is a pivotal package that defines the concept of a geographical `Region` and contains the core logic for the two routing algorithms.
  * **`simulator.topology`**: This package and its sub-packages are responsible for generating the broker network topology using a factory pattern.
  * **`simulator.population`**: This package handles the placement of subscribers and publishers onto the leaf brokers of a generated topology.
  * **`simulator.simulations`**: This package contains the main entry points for running different types of simulations, categorised into `functional` and `performance` sub-packages.

### Routing Algorithms

#### Region-Based Routing

This algorithm operates on the principle of subscriptions to geographical areas. A `SubscriberWithLocation` creates a `SubscriptionWithRegion` defining an area of interest. This is propagated up the broker hierarchy, with redundant, overlapping subscriptions being filtered out. When a `PublicationWithLocation` is sent, brokers forward it to children whose subscription regions contain the publication's location.

#### Location-Based Routing

This algorithm is designed to deliver the *closest* publication to a subscriber. A `SubscriberWithLocation` sends a `SubscriptionWithLocation` containing its own location. As a `PublicationWithLocation` travels down the broker tree, each broker maintains a cache of the closest publication it has seen and only forwards the publication if it is an "improvement" for its region.

### Executing Simulations and Tests

Running a simulation involves a clear, three-step process orchestrated by a `SimulationRunner`.

1.  **Topology Generation**: A `TopologyFactory` creates the broker network based on a `TopologyConfiguration` and a `BrokerFactory` that specifies the routing algorithm.
2.  **Client Attachment (Population)**: A `TopologyPopulator` uses placement strategies to attach subscribers and publishers to the network.
3.  **Scenario Execution and Metrics Collection**: The `executeScenarios` method runs the simulation, after which `collectAndPrintMetrics` gathers statistics on performance and overhead.

### Topology Types

The simulator can generate several types of network topologies:

  * **Fixed Topology**: A small, hardcoded hierarchy for **functional testing**.
  * **Grid Topology**: A configurable grid of leaf brokers, also for **functional testing**.
  * **Random Topology**: A random, tree-like topology for **performance simulations**.
  * **File-Based (GeoNames) Topology**: A realistic topology built from real-world geographical and population data for **large-scale performance simulations**.

### The Topology Visualiser

The simulator includes a powerful visualisation tool to help understand the system's behaviour. The `TopologyVisualiser` uses the `GraphStream` library to create a graphical representation of the broker network. It visualises the network structure and traces the flow of messages (subscriptions and publications) by highlighting their paths. The visualiser is intended for **functional tests**, where its step-by-step view is invaluable for debugging and verification. It is enabled by using the `VisualisedSimulationRunner`.

### Extending the Simulator

The simulator's modular design makes it straightforward to extend:

  * **Adding a New Routing Algorithm**: Create new `Broker` and `LeafBroker` classes, and a new `BrokerFactory` to instantiate them.
  * **Adding a New Topology**: Extend `AbstractTopologyFactory` and implement the required methods to build your custom network structure.
  * **Adding a New Client Placement Strategy**: Implement the `SubscribersPlacementStrategy` or `PublishersPlacementStrategy` interface and provide your new strategy to the `TopologyPopulator`.

### Running a Geographical Simulation

The easiest way to run a simulation is through the main menu provided in `simulator.Main`. When you run this class, it presents a text-based menu in the console, allowing you to choose from pre-configured functional tests and performance simulations.

For more **customised runs**, you can use the placeholder `main` methods provided in the simulation suite classes. These classes act as direct entry points for specific types of simulations.

  * **Functional Tests**: To run these, you can modify and execute `RegionFunctionalTestsMain.java` or `LocationFunctionalTestsMain.java`.
  * **Performance Simulations**: For these, you can modify and execute `RegionPerformanceSimulationsMain.java` or `LocationPerformanceSimulationsMain.java`.

To **run a simulation**, open one of the main simulation classes, such as `RegionPerformanceSimulationsMain.java`. These classes often contain commented-out calls to different simulation configurations, making it easy to switch between them.

```java
public class RegionPerformanceSimulationsMain {
    public static void main(String[] args) {
        // Run a smaller-scale performance test on a random topology
        //RandomTopologyRegionPerformanceSimulation.main(args);
        
        // Uncomment the line below to run the full-scale, realistic performance test
        GeoNamesBasedRegionPerformanceSimulation.main(args);
    }
}
```

If needed, open the specific simulation class being called (e.g., `GeoNamesBasedRegionPerformanceSimulation.java`) and **adjust its parameters**, such as the number of subscribers. Then rebuild the project (if changes were made to the source code) with `ant jar` and run the main class you selected (e.g., `RegionPerformanceSimulationsMain`) from the command line.

```console
# To run the placeholder for region-based performance simulations
$ java -cp build/jar/DNSPlus.jar simulator.RegionPerformanceSimulationsMain
```

The simulation will execute and print its results to the console.

```console
--- Starting Random Topology Performance Simulation (Region-Based) ---
--- Setting Global Log Level to: INFO ---

--- Initialising Simulation Runner ---
Using Factory: RandomTopologyGenerator
Using Configuration: RegionRandomTopologyConfiguration

--- Generating Topology ---
--- Topology Generation Complete ---
Root Node: Broker-0 (BrokerWithRegionProcessingRegion)

--- Populating Topology for Performance Simulation ---

--- Starting Proportional Subscriber Placement ---
Distributing 1000 total subscribers...
--- Proportional Subscriber Placement Complete. Total subscribers created: 1000 ---

--- Starting Hub-Based Publisher Placement (for Global Services) ---
Distributing 10 publishers among 1 hub regions...
--- Hub-Based Publisher Placement Complete. Total publishers created: 10 ---
Collected 1000 subscribers and 10 publishers.

--- Executing Region-Based Performance Scenario ---

>>> Phase 1: Subscribers are sending region-based subscriptions... <<<
  ... all subscriptions sent.

>>> Phase 2: All service replicas are sending their publications... <<<

--- Simulation Metrics ---
--- System Overhead Metrics ---
Total Subscription Table Entries Created (Propagation Cost): 1037
Total Region Boundary Updates: 25

--- Service Delivery Metrics ---
Total Publications Sent by all Replicas: 10
Total Successful Notifications Received by Subscribers: 40
Subscriber Match Rate: 0.40%

--- Simulation Run Finished ---
```