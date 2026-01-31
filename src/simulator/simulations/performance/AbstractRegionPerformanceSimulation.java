package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import simulator.config.BrokerConfig;
import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.core.WorkloadRepository;
import simulator.entities.SubscriberWithLocation;
import simulator.entities.context.RegionalEvaluationContext;
import simulator.events.PublicationWithLocation;
import simulator.regions.BoundedBroker;
import simulator.simulations.performance.metrics.MetricsCollector;
import simulator.simulations.performance.metrics.MetricsPrinter;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.RegionMetricsCollector;
import simulator.simulations.performance.metrics.RegionMetricsPrinter;
import simulator.simulations.performance.metrics.RegionPerformanceMetricsData;
import simulator.simulations.performance.metrics.groundtruth.GroundTruthCalculator;
import simulator.simulations.performance.metrics.groundtruth.RegionGroundTruthCalculator;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.workload.RegionWorkloadGenerator;
import simulator.workload.SubscriptionWorkloadOrchestrator;
import utils.CustomLogger;

public abstract class AbstractRegionPerformanceSimulation<C extends TopologyConfiguration, F extends AbstractTopologyFactory<C, BoundedBroker>> extends AbstractPerformanceSimulation<C, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractRegionPerformanceSimulation.class.getName());
    
    private final RegionWorkloadGenerator workloadGenerator = new RegionWorkloadGenerator();
    private final SubscriptionWorkloadOrchestrator orchestrator = new SubscriptionWorkloadOrchestrator();

    public AbstractRegionPerformanceSimulation() {
    }
    
    protected abstract List<BoundedBroker> getInterestHotspots(BoundedBroker root);

    // --- Polymorphic Factories ---
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
    protected MetricsCollector createMetricsCollector() {
        return new RegionMetricsCollector();
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

    @Override
    protected void executeScenarios() {
        logSectionHeader("Executing Region-Based Performance Scenario");

        if (allSubscribers.isEmpty()) return;

        // Explicitly inject Regional Logic (Good Practice)
        for (SubscriberWithLocation sub : allSubscribers) {
            sub.setEvaluationContext(new RegionalEvaluationContext());
        }

        List<BoundedBroker> leafBrokers = TopologyAnalyser.findLeafBrokers(this.rootNode);
        
        List<BoundedBroker> hotspots = getInterestHotspots(this.rootNode);
        workloadGenerator.setHotspots(hotspots);
        if (hotspots != null) {
            logger.info("Configured Workload Generator with " + hotspots.size() + " Interest Hotspots.");
        } else {
            logger.info("Configured Workload Generator with NO Hotspots (Pure Random Remote).");
        }
        
        logger.info("Pre-generating " + allPublishers.size() + " publications for Ground Truth context...");
        List<PublicationWithLocation> preGeneratedPubs = new ArrayList<>();
        for(var p : allPublishers) {
            PublicationWithLocation pub = new PublicationWithLocation(p.getLocation());
            pub.setSource(p); 
            preGeneratedPubs.add(pub);
        }

        logger.info("");
        logger.info(">>> Phase 1: Subscriptions (Streaming Batch Mode) ... <<<");

        orchestrator.generateDispatchAndCalculate(
            allSubscribers, 
            leafBrokers, 
            workloadGenerator,
            this.metricsData,
            preGeneratedPubs,
            this.truthCalculator 
        );

        logger.info("");
        logger.info(">>> Phase 2: Publications... <<<");
        
        for (int i = 0; i < preGeneratedPubs.size(); i++) {
            allPublishers.get(i).send(preGeneratedPubs.get(i));
        }
        
        collectAndPrintMetrics();
        WorkloadRepository.reset();
    }
}