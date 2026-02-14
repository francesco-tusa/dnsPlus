package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import simulator.config.BrokerConfig;
import simulator.config.SimConfiguration;
import simulator.core.WorkloadRepository;
import simulator.events.PublicationWithLocation;
import simulator.regions.BoundedBroker;
import simulator.simulations.performance.metrics.MetricsPrinter;
import simulator.simulations.performance.metrics.PerformanceMetricsData;
import simulator.simulations.performance.metrics.ProximityMetricsPrinter;
import simulator.simulations.performance.metrics.ProximityPerformanceMetricsData;
import simulator.simulations.performance.metrics.groundtruth.GroundTruthCalculator;
import simulator.simulations.performance.metrics.groundtruth.ProximityGroundTruthCalculator;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.workload.LocationWorkloadGenerator;
import simulator.workload.SubscriptionWorkloadGenerator;
import simulator.workload.SubscriptionWorkloadOrchestrator;
import utils.CustomLogger; 

public abstract class AbstractLocationPerformanceSimulation<C extends TopologyConfiguration, F extends AbstractTopologyFactory<C, BoundedBroker>> extends AbstractPerformanceSimulation<C, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractLocationPerformanceSimulation.class.getName());
    
    private final LocationWorkloadGenerator workloadGenerator = new LocationWorkloadGenerator();
    private final SubscriptionWorkloadOrchestrator orchestrator = new SubscriptionWorkloadOrchestrator();

    public AbstractLocationPerformanceSimulation() {
    }

    // --- Polymorphic Factories ---
    @Override
    protected SubscriptionWorkloadGenerator getWorkloadGenerator() {
        return new LocationWorkloadGenerator();
    }

    @Override
    protected PerformanceMetricsData createMetricsData() {
        return new ProximityPerformanceMetricsData();
    }

    @Override
    protected MetricsPrinter createMetricsPrinter() {
        return new ProximityMetricsPrinter(logger);
    }

    @Override
    protected GroundTruthCalculator createGroundTruthCalculator() {
        return new ProximityGroundTruthCalculator();
    }

    @Override
    protected void logSpecificConfiguration() {
        BrokerConfig brokerConfig = SimConfiguration.get().broker;
        
        logConfigItem("Routing Algorithm", "PROXIMITY (Closest Node)");
        logConfigItem("Brake Mechanism", brokerConfig.proximityBrakeEnabled ? "ENABLED" : "DISABLED");
        
        if (brokerConfig.proximityBrakeEnabled) {
            logConfigItem("Brake Limit (Burst Cap)", brokerConfig.proximityBrakeLimit);
            logConfigItem("Brake Interval", brokerConfig.proximityBrakeIntervalMs + " ms");
        }
    }
    
    @Override
    protected void executeScenarios() {
        logSectionHeader("Executing Location-Based Performance Scenario");

        if (allSubscribers.isEmpty()) return;

        List<BoundedBroker> leafBrokers = new ArrayList<>();
        for (var s : allSubscribers) {
            if (s.getBroker() instanceof BoundedBroker bb && !leafBrokers.contains(bb)) {
                leafBrokers.add(bb);
            }
        }

        logger.info("Pre-generating " + allPublishers.size() + " publications for Ground Truth context...");
        List<PublicationWithLocation> preGeneratedPubs = new ArrayList<>();

        for (var p : allPublishers) {
            PublicationWithLocation pub = new PublicationWithLocation(p.getLocation());
            pub.setSource(p);
            preGeneratedPubs.add(pub);
        }

        logger.info("");
        logger.info(">>> Phase 1: Subscriptions & GT Calculation... <<<");
        
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