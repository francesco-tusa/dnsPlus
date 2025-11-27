package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import simulator.config.BrokerConfig;
import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.core.SimulationRunner;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.population.ProportionalSubscribersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.population.TopologyPopulator;
import simulator.regions.BoundedBroker;
import simulator.simulations.performance.metrics.*;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.analysis.TopologyAnalyzer;
import simulator.visualisation.SimulationVisualiser;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public abstract class AbstractPerformanceSimulation<
    C extends TopologyConfiguration, 
    F extends AbstractTopologyFactory<C, BoundedBroker>
> extends SimulationRunner<C, BoundedBroker, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractPerformanceSimulation.class.getName());

    protected final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    protected final List<PublisherWithLocation> allPublishers = new ArrayList<>();
    
    protected final PerformanceMetricsData metricsData = new PerformanceMetricsData();

    protected abstract PublishersPlacementStrategy getPublisherPlacementStrategy();

    @Override
    protected Level getLogLevel() { return Level.INFO; }

    // --- Logging Helpers ---
    protected void printBanner(String t) {
        logger.info("==================================================================================");
        logger.info("  " + t);
        logger.info("==================================================================================");
    }
    protected void printSeparator() { logger.info("----------------------------------------------------------------------------------"); }
    protected void logConfigItem(String k, Object v) { logger.info(String.format("%-35s : %s", k, v)); }
    protected void logMetricItem(String k, Object v) { logger.info(String.format("%-40s : %s", k, v)); }

    @Override
    protected void initialise(F factory, C config) {
        super.initialise(factory, config);
        
        WorkloadConfig workload = SimConfiguration.get().workload;
        BrokerConfig brokerConfig = SimConfiguration.get().broker; 
        
        printBanner("SIMULATION CONFIGURATION");
        logConfigItem("Run ID", this.simulationTimestamp);
        logConfigItem("Topology Factory", factory.getClass().getSimpleName());
        config.logDetails(logger);

        logConfigItem("Broker Strategy", brokerConfig.strategy);
        if (brokerConfig.isSmartStrategy()) {
            logConfigItem("Smart Threshold", brokerConfig.smartThreshold);
        } else {
            logConfigItem("Smart Threshold", "N/A (Simple Mode)");
        }
        
        logConfigItem("Number of Replicas", workload.numberOfReplicas);
        logConfigItem("Subscribers per Replica", workload.subscribersPerReplica);
        logConfigItem("Total Subscribers", workload.getTotalSubscribers());
        logConfigItem("Publisher Strategy", getPublisherPlacementStrategy().getClass().getSimpleName());
        
        logSpecificConfiguration();
        printSeparator();
    }

    protected void logSpecificConfiguration() {}

    @Override
    protected void setupSimulation() {
        CsvMetricWriter.getInstance().initialize(this.simulationTimestamp);
        
        logSectionHeader("Populating Topology for Performance Simulation");

        if (this.rootNode == null) {
            logger.severe("Cannot populate topology: Root node is null.");
            return;
        }

        TopologyAnalyzer.logStructure(this.rootNode, logger);

        List<BoundedBroker> leafBrokers = TopologyAnalyzer.findLeafBrokers(this.rootNode);
        if (leafBrokers.isEmpty()) {
            logger.severe("Error: No leaf brokers found.");
            return;
        }
        
        WorkloadConfig workload = SimConfiguration.get().workload;
        TopologyPopulator populater = new TopologyPopulator(new ProportionalSubscribersPlacement(), getPublisherPlacementStrategy());
        populater.populate(this.rootNode, leafBrokers, workload.getTotalSubscribers(), workload.numberOfReplicas);
        
        collectClients(leafBrokers);
    }

    private void collectClients(List<BoundedBroker> leafBrokers) {
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
        
        // Merge subclass data
        collected.groundTruthMatches = this.metricsData.groundTruthMatches;
        
        MetricsPrinter printer = new MetricsPrinter(logger);
        printer.print(collected);
        
        logSpecificMetrics();
    }
    
    protected void logSpecificMetrics() {}

    @Override
    protected void cleanup() {
        super.cleanup();
        if (SimConfiguration.get().paths.enableVerboseLogs) {
            CsvMetricWriter.getInstance().close();
            logger.info("Metrics writer closed.");
        }
        SimulationVisualiser.getInstance().saveMapImage(simulationTimestamp);
        SimulationVisualiser.getInstance().close();
    }
}