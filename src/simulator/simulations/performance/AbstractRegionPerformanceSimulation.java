package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.regions.SubscriptionWithRegion;
import simulator.simulations.performance.metrics.GroundTruthCalculator;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.workload.RegionWorkloadGenerator;
import simulator.workload.SubscriptionWorkloadGenerator;
import utils.CustomLogger;

public abstract class AbstractRegionPerformanceSimulation<C extends TopologyConfiguration, F extends AbstractTopologyFactory<C, BoundedBroker>> extends AbstractPerformanceSimulation<C, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractRegionPerformanceSimulation.class.getName());
    
    // Concrete Region Generator
    private final RegionWorkloadGenerator workloadGenerator = new RegionWorkloadGenerator();

    public AbstractRegionPerformanceSimulation() {
        // No-arg constructor
    }
    
    /**
     * Abstract method to identify which brokers constitute "Hotspots".
     * @param root The topology root.
     * @return List of hotspot brokers (e.g. AWS regions or Top-Pop cities).
     */
    protected abstract List<BoundedBroker> getInterestHotspots(BoundedBroker root);

    @Override
    protected void logSpecificConfiguration() {
        WorkloadConfig w = SimConfiguration.get().workload;
        logConfigItem("Subscription Region Size", w.subscriptionRegionSize);
        logConfigItem("Remote Interest Probability", w.remoteInterestProbability);
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
        
        List<SubscriptionWithRegion> tempSubs = new ArrayList<>();

        logger.info("");
        logger.info(">>> Phase 1: Subscriptions... <<<");

        int interval = (int) Math.max(1000, w.getTotalSubscribers() / 10);
        
        for (int i = 0; i < allSubscribers.size(); i++) {
            SubscriberWithLocation s = allSubscribers.get(i);
            
            SimulationSubscription sub = workloadGenerator.generateSubscription(s, leafBrokers);
            
            if (sub instanceof SubscriptionWithRegion swr) {
                tempSubs.add(swr);
                s.send(swr);
            }
            
            if ((i + 1) % interval == 0) logger.info(String.format("  ... %d / %d", (i+1), allSubscribers.size()));
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