package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import simulator.config.SimConfiguration;
import simulator.config.WorkloadConfig;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CustomLogger;

public abstract class AbstractRegionPerformanceSimulation<
    C extends TopologyConfiguration,
    F extends AbstractTopologyFactory<C, BoundedBroker>
> extends AbstractPerformanceSimulation<C, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractRegionPerformanceSimulation.class.getName());

    private long groundTruthMatches = 0;

    @Override
    protected void logSpecificConfiguration() {
        WorkloadConfig workload = SimConfiguration.get().workload;
        logConfigItem("Subscription Region Size", workload.subscriptionRegionSize);
        logConfigItem("Remote Interest Probability", workload.remoteInterestProbability);
    }

    @Override
    protected void logSpecificMetrics() {
        logger.info("\nRegion-Specific Accuracy:");
        logMetricItem("Ground Truth (Potential) Matches", this.groundTruthMatches);
        
        if (this.groundTruthMatches > 0) {
            double accuracy = (double) successfulNotifications / this.groundTruthMatches * 100.0;
            logMetricItem("Delivery Accuracy", String.format("%.2f%%", accuracy));
        } else {
            logMetricItem("Delivery Accuracy", "N/A (0 matches)");
        }
    }

    @Override
    protected void executeScenarios() {
        WorkloadConfig workload = SimConfiguration.get().workload;

        logger.info("\n--- Executing Region-Based Performance Scenario ---");
        if (allSubscribers.isEmpty() || allPublishers.isEmpty()) {
            logger.severe("No subscribers or publishers were created. Cannot run scenarios.");
            return;
        }
        List<BoundedBroker> leafBrokers = findLeafBrokers(this.rootNode);

        // Phase 1: Subscriptions
        logger.info("\n>>> Phase 1: Subscribers are sending region-based subscriptions... <<<");
        final int PROGRESS_INTERVAL = (int) Math.max(1000, workload.getTotalSubscribers() / 10);
        
        List<SubscriptionWithRegion> tempSubsForGroundTruth = new ArrayList<>();

        for (int i = 0; i < allSubscribers.size(); i++) {
            SubscriberWithLocation subscriber = allSubscribers.get(i);
            SubscriptionWithRegion subscription = generateSubscriptionForSubscriber(subscriber, leafBrokers);
            
            // Store logic-only copy (lightweight) for ground truth if needed
            tempSubsForGroundTruth.add(subscription);
            
            subscriber.send(subscription); 
            
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
            
            publisher.send(pub); 
        }
        
        // Calculate Ground Truth
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
    
    protected SubscriptionWithRegion generateSubscriptionForSubscriber(SubscriberWithLocation subscriber, List<BoundedBroker> allLeafBrokers) {
        WorkloadConfig workload = SimConfiguration.get().workload;

        Region subscriptionRegion;
        if (random.nextDouble() < workload.remoteInterestProbability) {
            List<BoundedBroker> hubs = findTopDataCenters(allLeafBrokers, 30);
             if (hubs.isEmpty()) {
                 BoundedBroker randomBroker = allLeafBrokers.get(random.nextInt(allLeafBrokers.size()));
                 subscriptionRegion = new Region(randomBroker.getRegion());
            } else {
                 BoundedBroker remoteHub = hubs.get(random.nextInt(hubs.size()));
                 subscriptionRegion = new Region(remoteHub.getRegion());
            }
        } else {
            Location centerOfInterest = subscriber.getLocation();
            double halfSize = workload.subscriptionRegionSize / 2.0; 
            subscriptionRegion = new Region(
                new Location(centerOfInterest.getX() - halfSize, centerOfInterest.getY() - halfSize, 0),
                new Location(centerOfInterest.getX() + halfSize, centerOfInterest.getY() + halfSize, 0)
            );
        }
        return new SubscriptionWithRegion(subscriptionRegion);
    }

    protected List<BoundedBroker> findTopDataCenters(List<BoundedBroker> leafBrokers, int maxDCs) {
         return leafBrokers.stream()
            .sorted(Comparator.comparingLong(BoundedBroker::getInternetPopulation).reversed())
            .limit(Math.min(leafBrokers.size(), maxDCs))
            .collect(Collectors.toList());
    }
}