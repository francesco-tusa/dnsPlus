package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.core.SimulationRunner;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.population.PopulationBasedSubscribersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.population.SubscribersPlacementStrategy;
import simulator.population.TopologyPopulator;
import simulator.regions.BoundedBroker;
import simulator.simulations.performance.metrics.MetricsCollector;
import simulator.simulations.performance.metrics.MetricsPrinter;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.groundtruth.GroundTruthCalculator;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.workload.SubscriptionWorkloadGenerator;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public abstract class AbstractPerformanceSimulation<
    C extends TopologyConfiguration, 
    F extends AbstractTopologyFactory<C, BoundedBroker>
> extends SimulationRunner<C, BoundedBroker, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractPerformanceSimulation.class.getName());

    protected final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    protected final List<PublisherWithLocation> allPublishers = new ArrayList<>();
    
    protected PerformanceMetricsData metricsData;
    protected GroundTruthCalculator truthCalculator;
    protected PerformanceMetricsData lastRunMetrics;

    protected abstract SubscriptionWorkloadGenerator getWorkloadGenerator();
    protected abstract PerformanceMetricsData createMetricsData();
    protected abstract MetricsPrinter createMetricsPrinter();
    protected abstract GroundTruthCalculator createGroundTruthCalculator();
    protected abstract PublishersPlacementStrategy getPublisherPlacementStrategy();

    protected SubscribersPlacementStrategy getSubscriberPlacementStrategy() {
        return new PopulationBasedSubscribersPlacement();
    }
    
    protected CsvMetricWriter.TraceMetricStrategy getTraceMetricStrategy() {
        return new CsvMetricWriter.RegionTraceStrategy();
    }

    public PerformanceMetricsData getLastRunMetrics() { return lastRunMetrics; }

    @Override
    protected Level getLogLevel() { return Level.INFO; }

    protected void printBanner(String t) {
        logger.info("\n==================================================================================\n  " + t + "\n==================================================================================");
    }
    
    protected void printSeparator() { logger.info("----------------------------------------------------------------------------------"); }
    
    protected void logConfigItem(String k, Object v) { logger.info(String.format("%-35s : %s", k, v)); }

    @Override
    protected void initialise(F factory, C config) {
        this.metricsData = createMetricsData();
        this.truthCalculator = createGroundTruthCalculator();

        super.initialise(factory, config);
        
        printBanner("SIMULATION CONFIGURATION");
        logConfigItem("Run ID", this.simulationTimestamp);
        logConfigItem("Simulation Seed", SimConfiguration.get().simulationSeed);
        logConfigItem("Topology Factory", factory.getClass().getSimpleName());
        config.logDetails(logger);
        
        logWorkloadConfiguration(); 
        
        logSpecificConfiguration();
        printSeparator();
    }

    /**
     * Default workload logging. Subclasses can override this to print different info.
     */
    protected void logWorkloadConfiguration() {
        WorkloadConfig workload = SimConfiguration.get().workload;
        logConfigItem("Number of Replicas", workload.numberOfReplicas);
        if (workload.isBatchMode()) {
            logConfigItem("Subscribers per Replica", "IGNORED (Batch Override)");
        } else {
            logConfigItem("Subscribers per Replica", workload.subscribersPerReplica);
        }
        logConfigItem("Total Subscribers", workload.getTotalSubscribers());
        logConfigItem("Avg Subscriptions per Subscriber", workload.meanSubscriptionsPerSubscriber);
        logConfigItem("Arrival Distribution", workload.arrivalDistribution);
        
        try {
            logConfigItem("Publisher Strategy", getPublisherPlacementStrategy().getClass().getSimpleName());
        } catch (Exception e) {
            logConfigItem("Publisher Strategy", "Unknown (Init Error)");
        }
    }

    protected abstract void logSpecificConfiguration();

    @Override
    protected void setupSimulation() {
        boolean enableTracing = SimConfiguration.get().paths.enableEventTracing;
        
        // --- CHANGED: Use the extensible strategy method ---
        CsvMetricWriter.getInstance().initialize(
            this.simulationTimestamp, 
            enableTracing,
            getTraceMetricStrategy() 
        );
        
        if (enableTracing) {
            logger.info("Subscription Tracing: ENABLED (CSV files will be generated)");
        } else {
            logger.info("Subscription Tracing: DISABLED (Stats only mode)");
        }

        logSectionHeader("Populating Topology for Performance Simulation");

        if (this.rootNode == null) {
            logger.severe("Cannot populate topology: Root node is null.");
            return;
        }

        TopologyAnalyser.logStructure(this.rootNode, logger);

        List<BoundedBroker> leafBrokers = TopologyAnalyser.findLeafBrokers(this.rootNode);
        if (leafBrokers.isEmpty()) {
            logger.severe("Error: No leaf brokers found.");
            return;
        }
        
        WorkloadConfig workload = SimConfiguration.get().workload;
        TopologyPopulator populater = new TopologyPopulator(getSubscriberPlacementStrategy(), getPublisherPlacementStrategy());
        populater.populate(this.rootNode, leafBrokers, workload.getTotalSubscribers(), workload.numberOfReplicas);
        
        collectClients(leafBrokers);
    }

    protected void collectClients(List<BoundedBroker> leafBrokers) {
        allSubscribers.clear();
        allPublishers.clear();
        
        for (BoundedBroker leaf : leafBrokers) { 
            for (Object child : leaf.getChildren()) {
                if (child instanceof SubscriberWithLocation s) allSubscribers.add(s);
                else if (child instanceof PublisherWithLocation p) allPublishers.add(p);
            }
        }
        
        logger.info("Collected " + allSubscribers.size() + " subscribers and " + allPublishers.size() + " publishers.");
    }

    protected void collectAndPrintMetrics() {
        MetricsCollector collector = new MetricsCollector();
        PerformanceMetricsData collected = collector.collect(this.rootNode, allSubscribers, allPublishers);
        collected.groundTruthMatches = this.metricsData.groundTruthMatches;
        this.lastRunMetrics = collected;
        MetricsPrinter printer = createMetricsPrinter();
        printer.print(collected);        
    }
    
    @Override
    protected void cleanup() {
        super.cleanup();
        CsvMetricWriter.getInstance().close();
        if (SimConfiguration.get().paths.enableVerboseLogs) {
            logger.info("Metrics writer closed.");
        }
    }
}