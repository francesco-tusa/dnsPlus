package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import simulator.config.BrokerConfig;
import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.core.WorkloadRepository;
import simulator.entities.PublisherWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.regions.BoundedBroker;
import simulator.simulations.performance.metrics.MetricsPrinter;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.RegionMetricsPrinter;
import simulator.simulations.performance.metrics.RegionPerformanceMetricsData;
import simulator.simulations.performance.metrics.groundtruth.GroundTruthCalculator;
import simulator.simulations.performance.metrics.groundtruth.RegionGroundTruthCalculator;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.workload.RegionWorkloadGenerator;
import simulator.workload.SubscriptionWorkloadGenerator;
import simulator.workload.SubscriptionWorkloadOrchestrator;
import utils.CustomLogger;

public abstract class AbstractRegionPerformanceSimulation<C extends TopologyConfiguration, F extends AbstractTopologyFactory<C, BoundedBroker>> extends AbstractPerformanceSimulation<C, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractRegionPerformanceSimulation.class.getName());
    
    private final SubscriptionWorkloadOrchestrator orchestrator = new SubscriptionWorkloadOrchestrator();

    public AbstractRegionPerformanceSimulation() {
    }
    
    protected abstract List<BoundedBroker> getInterestHotspots(BoundedBroker root);

    // --- Polymorphic Factories ---
    @Override
    protected SubscriptionWorkloadGenerator getWorkloadGenerator() {
        return new RegionWorkloadGenerator();
    }

    @Override
    protected PerformanceMetricsData createMetricsData() {
        return new RegionPerformanceMetricsData();
    }

    @Override
    protected MetricsPrinter createMetricsPrinter() {
        return new RegionMetricsPrinter(logger);
    }

    @Override
    protected GroundTruthCalculator createGroundTruthCalculator() {
        return new RegionGroundTruthCalculator();
    }

    @Override
    protected void logSpecificConfiguration() {
        BrokerConfig brokerConfig = SimConfiguration.get().broker;
        logConfigItem("Routing Algorithm", "SPATIAL MATCH (Region Overlap)");
        logConfigItem("Broker Strategy", brokerConfig.strategy);
        
        if (brokerConfig.isSmartStrategy()) {
            logConfigItem("Smart Threshold", brokerConfig.getSmartThreshold());
        } else {
            logConfigItem("Smart Threshold", "N/A (Simple Mode)");
        }
        logConfigItem("Intersection Optimization", brokerConfig.isIntersectionOptimizationEnabled());

        WorkloadConfig w = SimConfiguration.get().workload;
        logConfigItem("Subscription Region Size", w.subscriptionRegionSize);
        logConfigItem("Remote Interest Probability", w.remoteInterestProbability);
        logConfigItem("Density Skew Enabled", w.enableDensitySkew);
    }

    /**
     * Generates the list of publications to be used in the simulation.
     * <p>
     * Default implementation creates standard PublicationWithLocation events.
     * Subclasses (like Marketplace) can override this to create specialized events (e.g., ServiceRequests).
     * </p>
     * @param publishers The list of active publishers (clients).
     * @return A list of publication events ready for dispatch.
     */
    protected List<PublicationWithLocation> generatePublications(List<PublisherWithLocation> publishers) {
        logger.info("Pre-generating " + publishers.size() + " publications for Ground Truth context...");
        List<PublicationWithLocation> pubs = new ArrayList<>();
        for(var p : publishers) {
            PublicationWithLocation pub = new PublicationWithLocation(p.getLocation());
            pub.setSource(p); 
            pubs.add(pub);
        }
        return pubs;
    }

    @Override
    protected void executeScenarios() {
        logSectionHeader("Executing Region-Based Performance Scenario");

        if (allSubscribers.isEmpty()) return;
        
        List<BoundedBroker> leafBrokers = TopologyAnalyser.findLeafBrokers(this.rootNode);
        
        List<BoundedBroker> hotspots = getInterestHotspots(this.rootNode);
        getWorkloadGenerator().setHotspots(hotspots);
        if (hotspots != null) {
            logger.info("Configured Workload Generator with " + hotspots.size() + " Interest Hotspots.");
        } else {
            logger.info("Configured Workload Generator with NO Hotspots (Pure Random Remote).");
        }
        
        List<PublicationWithLocation> preGeneratedPubs = generatePublications(allPublishers);

        logger.info("");
        logger.info(">>> Phase 1: Subscriptions (Streaming Batch Mode) ... <<<");

        orchestrator.generateDispatchAndCalculate(
            allSubscribers, 
            leafBrokers, 
            getWorkloadGenerator(),
            this.metricsData,
            preGeneratedPubs,
            this.truthCalculator 
        );

        logger.info("");
        logger.info(">>> Phase 2: Publications... <<<");
        
        for (int i = 0; i < preGeneratedPubs.size(); i++) {
            // Ensure source is set (redundant check for safety)
            if (preGeneratedPubs.get(i).getSource() == null) {
                preGeneratedPubs.get(i).setSource(allPublishers.get(i));
            }
            allPublishers.get(i).send(preGeneratedPubs.get(i));
        }
        
        collectAndPrintMetrics();
        WorkloadRepository.reset();
    }
}