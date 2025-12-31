package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.core.WorkloadRepository;
import simulator.events.PublicationWithLocation;
import simulator.regions.BoundedBroker;
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
        
        // 0. Pre-generate Publication Objects (for Ground Truth calculation)
        // We generate the data now, but we do NOT send them yet.
        logger.info("Pre-generating " + allPublishers.size() + " publications for Ground Truth context...");
        List<PublicationWithLocation> preGeneratedPubs = new ArrayList<>();
        for(var p : allPublishers) {
            // We assume 1 pub per publisher for this scenario as per original code
            PublicationWithLocation pub = new PublicationWithLocation(p.getLocation());
            preGeneratedPubs.add(pub);
        }

        logger.info("");
        logger.info(">>> Phase 1: Subscriptions (Streaming Batch Mode) ... <<<");

        // 1. Streaming Generation & Dispatching
        // This method handles:
        //    - Batching subscribers (Randomized)
        //    - Generating subs into WorkloadRepository
        //    - Calculating Ground Truth (Parallel Thread)
        //    - Dispatching (Main Thread)
        //    - Clearing Repository
        orchestrator.generateDispatchAndCalculate(
            allSubscribers, 
            leafBrokers, 
            workloadGenerator,
            this.metricsData,
            preGeneratedPubs
        );

        logger.info("");
        logger.info(">>> Phase 2: Publications... <<<");
        
        // 2. Actually send the publications through the network
        // Note: We reuse the preGeneratedPubs to ensure consistency if IDs were involved, 
        // though here we just need to ensure the logic matches.
        int pubIndex = 0;
        for(var p : allPublishers) {
            if (pubIndex < preGeneratedPubs.size()) {
                p.send(preGeneratedPubs.get(pubIndex));
            } else {
                // Fallback if mismatch (shouldn't happen)
                p.send(new PublicationWithLocation(p.getLocation()));
            }
            pubIndex++;
        }
        
        // 3. Metrics Collection
        // Note: Ground Truth matches were already calculated incrementally in Phase 1.
        collectAndPrintMetrics();
        
        // 4. Final Cleanup
        WorkloadRepository.reset();
    }
}