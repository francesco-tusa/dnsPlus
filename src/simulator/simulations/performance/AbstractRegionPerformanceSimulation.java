package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger; // Import Logger
import java.util.stream.Collectors;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CustomLogger; // Import CustomLogger

public abstract class AbstractRegionPerformanceSimulation<
    C extends TopologyConfiguration,
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends AbstractPerformanceSimulation<C, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractRegionPerformanceSimulation.class.getName());

    protected final double subscriptionRegionSize; 
    protected final double remoteInterestProbability;

    // --- Lists to store all clients/messages for ground truth calculation ---
    protected final List<SubscriptionWithRegion> allSubscriptions = new ArrayList<>();
    protected final List<PublicationWithLocation> allPublications = new ArrayList<>();
    private long groundTruthMatches = 0;

    /**
     * Main constructor with all flags.
     * @param numberOfReplicas Total number of publisher replicas.
     * @param subscribersPerReplica Subscribers per replica ratio.
     * @param subscriptionRegionSize The absolute size (e.g., 10.0 for a 10x10 box) of local subscription regions.
     * @param remoteInterestProbability Probability of subscribing to a remote DC.
     * @param enableCsvOutput True to write raw metrics to CSV files.
     */
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
    
    /**
     * Constructor without CSV flag (defaults to false).
     */
    public AbstractRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                               double subscriptionRegionSize, double remoteInterestProbability) {
        this(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability, false);
    }
    
    protected double getSubscriptionRegionSize() { return subscriptionRegionSize; }
    protected double getRemoteInterestProbability() { return remoteInterestProbability; }

    /**
     * Override setupSimulation to log region-specific parameters.
     */
    @Override
    protected void setupSimulation() {
        super.setupSimulation(); // This logs the base parameters (total subs, replicas)
        
        // --- Log key parameters for context ---
        logger.info(String.format("Region Sim Setup: Subscription Region Size=%.2f, Remote Interest Probability=%.2f", 
                                  subscriptionRegionSize, remoteInterestProbability));
    }

    @Override
    protected void executeScenarios() {
        logger.info("\n--- Executing Region-Based Performance Scenario ---");
        if (allSubscribers.isEmpty() || allPublishers.isEmpty()) {
            logger.severe("No subscribers or publishers were created. Cannot run scenarios.");
            return;
        }
        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        
        // --- Clear lists for this run ---
        allSubscriptions.clear();
        allPublications.clear();

        logger.info("\n>>> Phase 1: Subscribers are sending region-based subscriptions... <<<");
        final int PROGRESS_INTERVAL = (int) Math.max(1000, getTotalSubscribers() / 10);
        for (int i = 0; i < allSubscribers.size(); i++) {
            SubscriberWithLocation subscriber = allSubscribers.get(i);
            SubscriptionWithRegion subscription = generateSubscriptionForSubscriber(subscriber, leafBrokers);
            
            // --- Add to list for ground truth calculation ---
            allSubscriptions.add(subscription); 
            
            subscriber.send(subscription);
            if ((i + 1) % PROGRESS_INTERVAL == 0 || (i+1) == allSubscribers.size()) {
                logger.info(String.format("  ... processed %d / %d subscriptions.", (i + 1), allSubscribers.size()));
            }
        }

        logger.info("\n>>> Phase 2: All service replicas are sending their publications... <<<");
        for(var publisher : allPublishers) {
            // --- Create and store publication for ground truth ---
            PublicationWithLocation pub = new simulator.events.PublicationWithLocation(publisher.getLocation());
            allPublications.add(pub);
            
            publisher.send(pub);
        }
        
        // --- Calculate ground truth before collecting metrics ---
        calculateGroundTruth();
        
        collectAndPrintMetrics(); // This is the method in AbstractPerformanceSimulation
    }
    
    /**
     * Helper method to calculate the ground truth.
     * This iterates through all publications and all subscriptions to find
     * the theoretical maximum number of matches.
     */
    private void calculateGroundTruth() {
        logger.info("\n--- Calculating Ground Truth Matches ---");
        this.groundTruthMatches = 0;
        if (allSubscriptions.isEmpty() || allPublications.isEmpty()) {
            logger.warning("Cannot calculate ground truth, no subscriptions or publications were generated.");
            return;
        }

        long pubCount = allPublications.size();
        long subCount = allSubscriptions.size();
        // Set a reasonable progress interval for the log
        long progressInterval = Math.max(1, (pubCount * subCount) / 10_000_000 / 10); // Aim for ~10 log lines
        if (progressInterval == 0) progressInterval = 1;

        // This is O(P*S), but it's the only way to get the true baseline.
        for (int i = 0; i < pubCount; i++) {
            PublicationWithLocation pub = allPublications.get(i);
            Location pubLoc = pub.getLocation();
            
            for (int j = 0; j < subCount; j++) {
                SubscriptionWithRegion sub = allSubscriptions.get(j);
                if (sub.getRegion().contains(pubLoc)) {
                    this.groundTruthMatches++;
                }
            }
            
            if ((i + 1) % progressInterval == 0 || (i + 1) == pubCount) {
                logger.info(String.format("  ... checked %d / %d publications against %d subscriptions.", (i+1), pubCount, subCount));
            }
        }
        logger.info("--- Ground Truth Calculation Complete: " + this.groundTruthMatches + " total potential matches. ---");
    }
    
    protected SubscriptionWithRegion generateSubscriptionForSubscriber(SubscriberWithLocation subscriber, List<BrokerWithRegion> allLeafBrokers) {
        Region subscriptionRegion;
        if (random.nextDouble() < getRemoteInterestProbability()) {
            // Remote interest logic (subscribing to a whole hub region) remains the same
            List<BrokerWithRegion> hubs = findTopDataCenters(allLeafBrokers, 30);
             if (hubs.isEmpty()) {
                 BrokerWithRegion randomBroker = allLeafBrokers.get(random.nextInt(allLeafBrokers.size()));
                 subscriptionRegion = new Region(randomBroker.getRegion());
            } else {
                 BrokerWithRegion remoteHub = hubs.get(random.nextInt(hubs.size()));
                 subscriptionRegion = new Region(remoteHub.getRegion());
            }
        } else {
            // Create a fixed-size region centered on the subscriber's location.
            Location centerOfInterest = subscriber.getLocation();
            double halfSize = getSubscriptionRegionSize() / 2.0; // Use the absolute size
            
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

    /**
     * Adds region-specific and ground-truth metrics.
     */
    @Override
    protected void collectAndPrintMetrics() {
        super.collectAndPrintMetrics(); // This prints all the base metrics

        // --- Print region-specific and ground truth metrics ---
        logger.info("\n--- Region-Specific Delivery Metrics ---");
        logger.info(String.format("Region Sim Setup: Subscription Region Size=%.2f, Remote Interest Probability=%.2f", 
                                  subscriptionRegionSize, remoteInterestProbability));

        logger.info("Ground Truth (Potential) Matches: " + this.groundTruthMatches);

        if (this.groundTruthMatches > 0) {
            double accuracy = (double) successfulNotifications / this.groundTruthMatches * 100.0;
            logger.info(String.format("Delivery Accuracy (Notifications / Ground Truth): %.2f%%", accuracy));
        }

        if (getTotalSubscribers() > 0) {
            long matchedSubscribers = allSubscribers.stream().filter(s -> s.getnPublications() > 0).count();
            double matchRate = (double) matchedSubscribers / getTotalSubscribers() * 100.0;
            logger.info(String.format("Subscriber Match Rate (Subscribers with >0 msgs): %.2f%% (%d / %d)", 
                                      matchRate, matchedSubscribers, getTotalSubscribers()));
            
            if (matchedSubscribers > 0) {
                double avgPubsPerMatchedSub = (double) successfulNotifications / matchedSubscribers;
                logger.info(String.format("Average Notifications per Matched Subscriber: %.2f (%d / %d)", 
                                          avgPubsPerMatchedSub, successfulNotifications, matchedSubscribers));
            }
        }
    }
}