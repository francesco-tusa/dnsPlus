package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.regions.SubscriptionWithRegion;
import simulator.simulations.performance.metrics.GroundTruthCalculator;
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

    @Override
    protected void logSpecificConfiguration() {
        WorkloadConfig w = SimConfiguration.get().workload;
        logConfigItem("Subscription Region Size", w.subscriptionRegionSize);
        logConfigItem("Remote Interest Probability", w.remoteInterestProbability);
        logConfigItem("Density Skew Enabled", w.enableDensitySkew);
    }

    @Override
    protected void executeScenarios() {
        WorkloadConfig w = SimConfiguration.get().workload;

        logSectionHeader("Executing Region-Based Performance Scenario");

        if (allSubscribers.isEmpty()) return;
        
        List<BoundedBroker> leafBrokers = TopologyAnalyser.findLeafBrokers(this.rootNode);
        
        List<BoundedBroker> hotspots = getInterestHotspots(this.rootNode);
        workloadGenerator.setHotspots(hotspots);
        if (hotspots != null) {
            logger.info("Configured Workload Generator with " + hotspots.size() + " Interest Hotspots.");
        } else {
            logger.info("Configured Workload Generator with NO Hotspots (Pure Random Remote).");
        }
        
        logger.info("");
        logger.info(">>> Phase 1: Subscriptions (Orchestrated) ... <<<");

        // Use Orchestrator to Generate, Skew, Shuffle, and Dispatch
        List<SimulationSubscription> allGeneratedSubs = orchestrator.generateAndDispatchWorkload(
            allSubscribers, 
            leafBrokers, 
            workloadGenerator
        );

        // Filter for Ground Truth
        List<SubscriptionWithRegion> tempSubs = new ArrayList<>();
        for (SimulationSubscription s : allGeneratedSubs) {
            if (s instanceof SubscriptionWithRegion swr) {
                tempSubs.add(swr);
            }
        }

        logger.info("");
        logger.info(">>> Phase 2: Publications... <<<");

        List<PublicationWithLocation> tempPubs = new ArrayList<>();
        for(var p : allPublishers) {
            PublicationWithLocation pub = new PublicationWithLocation(p.getLocation());
            tempPubs.add(pub);
            p.send(pub);
        }
        
        this.metricsData.groundTruthMatches = GroundTruthCalculator.calculateRegionMatches(tempSubs, tempPubs);
        collectAndPrintMetrics();
    }
}