package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public abstract class AbstractRegionPerformanceSimulation<
    C extends TopologyConfiguration,
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends AbstractPerformanceSimulation<C, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractRegionPerformanceSimulation.class.getName());

    protected final double subscriptionRegionSize; 
    protected final double remoteInterestProbability;

    // Keep lists of clients, but NOT the events themselves (to save memory)
    // Ground truth calculation will generate events on the fly if needed, 
    // or you can disable ground truth for very large simulations.
    private long groundTruthMatches = 0;

    public AbstractRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                               double subscriptionRegionSize, double remoteInterestProbability,
                                               boolean enableCsvOutput) {
        super(numberOfReplicas, subscribersPerReplica, enableCsvOutput); 
        
        if (subscriptionRegionSize <= 0) 
            throw new IllegalArgumentException("Subscription region size must be positive.");
        this.subscriptionRegionSize = subscriptionRegionSize;
        
        if (remoteInterestProbability < 0.0 || remoteInterestProbability > 1.0) 
            throw new IllegalArgumentException("Remote interest probability must be between 0.0 and 1.0.");
        this.remoteInterestProbability = remoteInterestProbability;
    }
    
    public AbstractRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                               double subscriptionRegionSize, double remoteInterestProbability) {
        this(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability, false);
    }
    
    protected double getSubscriptionRegionSize() { return subscriptionRegionSize; }
    protected double getRemoteInterestProbability() { return remoteInterestProbability; }

    @Override
    protected void setupSimulation() {
        super.setupSimulation();
        logSimulationParameters();
    }

    private void logSimulationParameters() {
        logger.info("\n--- Simulation Run Parameters (Run ID: " + this.simulationTimestamp + ") ---");
        logger.info("  Region Strategy:");
        logger.info(String.format("    - Subscription Region Size: %.2f", this.subscriptionRegionSize));
        logger.info(String.format("    - Remote Interest Probability: %.2f", this.remoteInterestProbability));

        logger.info("  Topology Configuration:");
        if (this.topologyConfig instanceof RegionRandomTopologyConfiguration config) {
            logger.info(String.format("    - Type: Random (Depth=%d, Branch=%d)", config.getTreeDepth(), config.getMaxBranchingFactor()));
        } else if (this.topologyConfig instanceof FileBasedTopologyConfiguration config) {
            logger.info(String.format("    - Type: File-Based (%s)", config.getTopologyFilePath()));
        }
        logger.info("--- End of Simulation Parameters ---");
    }

    @Override
    protected void executeScenarios() {
        logger.info("\n--- Executing Region-Based Performance Scenario ---");
        if (allSubscribers.isEmpty() || allPublishers.isEmpty()) {
            logger.severe("No subscribers or publishers were created. Cannot run scenarios.");
            return;
        }
        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);

        // Phase 1: Subscriptions
        logger.info("\n>>> Phase 1: Subscribers are sending region-based subscriptions... <<<");
        final int PROGRESS_INTERVAL = (int) Math.max(1000, getTotalSubscribers() / 10);
        
        // We need to store subscriptions TEMPORARILY for ground truth if needed,
        // but for pure performance, we might skip this to save RAM.
        // Here, I will regenerate them strictly for ground truth check later if required,
        // OR we assume ground truth is an offline check. 
        // For now, let's just SEND them.
        
        List<SubscriptionWithRegion> tempSubsForGroundTruth = new ArrayList<>();

        for (int i = 0; i < allSubscribers.size(); i++) {
            SubscriberWithLocation subscriber = allSubscribers.get(i);
            SubscriptionWithRegion subscription = generateSubscriptionForSubscriber(subscriber, leafBrokers);
            
            // Store logic-only copy (lightweight) for ground truth if needed
            tempSubsForGroundTruth.add(subscription);
            
            subscriber.send(subscription); // This triggers streaming logs
            
            if ((i + 1) % PROGRESS_INTERVAL == 0 || (i+1) == allSubscribers.size()) {
                logger.info(String.format("  ... processed %d / %d subscriptions.", (i + 1), allSubscribers.size()));
            }
        }

        // Phase 2: Publications
        logger.info("\n>>> Phase 2: All service replicas are sending their publications... <<<");
        
        List<PublicationWithLocation> tempPubsForGroundTruth = new ArrayList<>();
        
        for(var publisher : allPublishers) {
            PublicationWithLocation pub = new PublicationWithLocation(publisher.getLocation());
            tempPubsForGroundTruth.add(pub);
            
            publisher.send(pub); // This triggers streaming logs
        }
        
        // Calculate Ground Truth (Optional: disable for massive scales to save RAM)
        calculateGroundTruth(tempSubsForGroundTruth, tempPubsForGroundTruth);
        
        // Cleanup temp lists immediately
        tempSubsForGroundTruth.clear();
        tempPubsForGroundTruth.clear();
        
        // Finalize Metrics
        collectAndPrintMetrics();
    }
    
    /**
     * Calculates theoretical matches. O(S * P) complexity.
     */
    private void calculateGroundTruth(List<SubscriptionWithRegion> subs, List<PublicationWithLocation> pubs) {
        logger.info("\n--- Calculating Ground Truth Matches ---");
        this.groundTruthMatches = 0;
        if (subs.isEmpty() || pubs.isEmpty()) return;

        long pubCount = pubs.size();
        long subCount = subs.size();
        long progressInterval = Math.max(1, (pubCount * subCount) / 10_000_000 / 10); 

        for (int i = 0; i < pubCount; i++) {
            Location pubLoc = pubs.get(i).getLocation();
            for (SubscriptionWithRegion sub : subs) {
                if (sub.getRegion().contains(pubLoc)) {
                    this.groundTruthMatches++;
                }
            }
            if ((i + 1) % progressInterval == 0) {
                logger.info(String.format("  ... checked %d / %d publications.", (i+1), pubCount));
            }
        }
        logger.info("--- Ground Truth Calculation Complete: " + this.groundTruthMatches + " total potential matches. ---");
    }
    
    protected SubscriptionWithRegion generateSubscriptionForSubscriber(SubscriberWithLocation subscriber, List<BrokerWithRegion> allLeafBrokers) {
        Region subscriptionRegion;
        if (random.nextDouble() < getRemoteInterestProbability()) {
            List<BrokerWithRegion> hubs = findTopDataCenters(allLeafBrokers, 30);
             if (hubs.isEmpty()) {
                 BrokerWithRegion randomBroker = allLeafBrokers.get(random.nextInt(allLeafBrokers.size()));
                 subscriptionRegion = new Region(randomBroker.getRegion());
            } else {
                 BrokerWithRegion remoteHub = hubs.get(random.nextInt(hubs.size()));
                 subscriptionRegion = new Region(remoteHub.getRegion());
            }
        } else {
            Location centerOfInterest = subscriber.getLocation();
            double halfSize = getSubscriptionRegionSize() / 2.0; 
            subscriptionRegion = new Region(
                new Location(centerOfInterest.getX() - halfSize, centerOfInterest.getY() - halfSize, 0),
                new Location(centerOfInterest.getX() + halfSize, centerOfInterest.getY() + halfSize, 0)
            );
        }
        return new SubscriptionWithRegion(subscriptionRegion);
    }

    protected List<BrokerWithRegion> findTopDataCenters(List<BrokerWithRegion> leafBrokers, int maxDCs) {
         return leafBrokers.stream()
            .sorted(Comparator.comparingLong(BrokerWithRegion::getInternetPopulation).reversed())
            .limit(Math.min(leafBrokers.size(), maxDCs))
            .collect(Collectors.toList());
    }

    @Override
    protected void collectAndPrintMetrics() {
        // Only print high-level summary here.
        // Detailed logs are already on disk in CSVs.
        super.collectAndPrintMetrics(); 

        logger.info("\n--- Region-Specific Delivery Metrics ---");
        logger.info("Ground Truth (Potential) Matches: " + this.groundTruthMatches);
        
        if (this.groundTruthMatches > 0 && successfulNotifications > 0) {
            double accuracy = (double) successfulNotifications / this.groundTruthMatches * 100.0;
            logger.info(String.format("Delivery Accuracy (Notifications / Ground Truth): %.2f%%", accuracy));
        }
        
        // Explicitly close the writer
        CsvMetricWriter.getInstance().close();
        logger.info("Metrics streaming closed.");
    }
}