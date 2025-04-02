# Dynamic Directory Service with Homomorphic Encryption Based Privacy
This project aims to create a Directory Service that addresses dynamic information distribution via a *pub/sub* and provides *privacy* to users interacting with the system using *homomorphic encryption*.

---
## **Requirements**  
The project requires a version of the Java language **equal to or greater than 23**.  

If you have multiple versions of Java installed, please make sure to configure `JAVA_HOME` properly before compiling the project.  

### **macOS**  
On macOS, you can set `JAVA_HOME` to use Java 23 by running:  
```console
$ export JAVA_HOME=$(/usr/libexec/java_home -v 23)
```
To verify that Java 23 is being used, run:  
```console
$ java -version
```

### **Linux**  
On Linux, make sure `JAVA_HOME` is set correctly. If Java 23 is installed but not the default version, you can manually specify it:  
```console
$ export JAVA_HOME=/path/to/java23
$ export PATH=$JAVA_HOME/bin:$PATH
```
Replace `/path/to/java23` with the actual path where Java 23 is installed.  

To check the active Java version, run:  
```console
$ java -version
```

## **Build**  
Change the current directory to the one containing the project's source and run the command:  
```console
$ ant jar
```
This will **compile the source code** and generate the JAR file in the `build/jar` directory.

## **Run the Project**  

By default, the generated JAR file will run the `experiments.Main` class. You can execute it using:  
```console
$ java -jar build/jar/DNSPlus.jar
```

### **Run Other Classes**  

If you want to run a different class from the JAR (e.g., `simulator.Simulation`), you need to specify the JAR as part of the **classpath** using the `-cp` flag:  
```console
$ java -cp build/jar/DNSPlus.jar simulator.Simulation
```
Make sure that the class you want to run has a valid `public static void main(String[] args)` method.

## **Clean the Project**  
To remove all compiled files and generated JARs, run:  
```console
$ ant clean
```
This will delete the `build/` directory and all its contents.

To **clean and rebuild** the project in one step, run:  
```console
$ ant clean-build
```

---
## Defining Experiments
The current code provides a framework to define and execute various experiments using different configurations. Each experiment includes a Broker and a number of *Publishers* generating *notifications* about services and *Subscribers* interested in *subscribing* and receiving those *notifications*. The encryption mechanism is based on a pre-existing modified implementation of Paillier with a key length of `n=2048` bits.

Some placeholder classes are available in the codebase and can be used as templates for writing customised experiments. Please refer to the examples provided below.

### Entities

- **Publisher**: Represents an entity that generates and publishes content.
- **ReceivingSubscriber**: Represents an entity subscribing to content and receiving publications.
- **Broker**: Different types of brokers are available, but currently, the one to be used is the `AsynchronousBrokerWithBinaryBalancedTreeAndCache` class. This broker is based on multiple threads consuming publications and subscriptions from separate queues, plus a dispatcher delivering publications to the right subscribers according to the matching results. This implementation also provides a cache for publications the broker receives that are added to the cache if seen for the first time. When a subscription arrives, the content of the cache is checked for matching publications, and a result is immediately returned to a subscriber if possible.


### Three-phase Parallel Task Execution with Notification Caching

This experiment allows for the definition and running of experiments with various entities such as publishers and subscribers. Each entity can participate in tasks during three phases: preTask, task, and postTask. Operations in each phase are executed in parallel.


#### Task Phases

1. **preTask**: Tasks executed before the main experiment run.
2. **task**: Main tasks executed during the experiment run.
3. **postTask**: Tasks executed after the main experiment run.

#### Task Operations and Generation

Operations within a task can be specified in two ways:

1. **Explicitly**: By submitting a list of domain names to be used for either subscriptions or publications.
2. **Randomly**: When the random option is selected, a probability distribution is used based on the list of domains and their frequency (popularity) specified in the input file (an example is provided in `ranked_websites.csv`).

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
    taskGenerator.addTask(new ReceivingSubscriber("sub3"), new ArrayList<>(Arrays.asList("www.facebook.com")));
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

This example demonstrates how to set up publishers and subscribers, add tasks for different phases, and execute the experiment run. Each experiment is based on executing the operations defined in a corresponding run containing the task definition. At the end of an experiment, an average of the execution time of operations is provided as output.

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
