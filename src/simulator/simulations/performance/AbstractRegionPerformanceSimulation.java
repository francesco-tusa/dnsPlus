package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import utils.CustomLogger;

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

        // --- Log all parameters in a consolidated block ---
        logSimulationParameters();
    }


    /**
     * Logs all key simulation parameters in a single, consolidated block.
     */
    private void logSimulationParameters() {
        logger.info("\n--- Simulation Run Parameters (Run ID: " + this.simulationTimestamp + ") ---");
        
        // --- Client Configuration ---
        logger.info("  Client Configuration:");
        logger.info(String.format("    - Publisher Replicas: %d", this.numberOfReplicas));
        logger.info(String.format("    - Subscribers / Replica: %d", this.subscribersPerReplica));
        logger.info(String.format("    - Total Subscribers: %d", this.totalSubscribers));
        
        // --- Region Strategy Configuration ---
        logger.info("  Region Strategy:");
        logger.info(String.format("    - Subscription Region Size: %.2f", this.subscriptionRegionSize));
        logger.info(String.format("    - Remote Interest Probability: %.2f", this.remoteInterestProbability));

        // --- Topology Configuration ---
        logger.info("  Topology Configuration:");
        if (this.topologyConfig instanceof RegionRandomTopologyConfiguration config) {
            logger.info(String.format("    - Type: Random"));
            logger.info(String.format("    - Tree Depth: %d", config.getTreeDepth()));
            logger.info(String.format("    - Max Branching: %d", config.getMaxBranchingFactor()));
            logger.info(String.format("    - Region Count: %d", config.getNumRegions()));
        } else if (this.topologyConfig instanceof FileBasedTopologyConfiguration config) {
            logger.info(String.format("    - Type: File-Based"));
            logger.info(String.format("    - Source File: %s", config.getTopologyFilePath()));
        } else {
            logger.info("    - Type: Unknown");
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

    @Override
    protected void collectAndPrintMetrics() {
        // --- 1. Call parent to collect all base metrics ---
        super.collectAndPrintMetrics(); 

        // --- 2. Now, collect metrics SPECIFIC to this child class ---
        List<Integer> finalSubscriptionHops = new ArrayList<>();
        List<Map<String, Object>> subscriptionPathData = new ArrayList<>();

        // We iterate over the master list of ORIGINAL subscriptions
        for (SubscriptionWithRegion sub : allSubscriptions) {
            if (sub != null) {
                finalSubscriptionHops.add(sub.getHops());
                
                Map<String, Object> row = new HashMap<>();
                row.put("subscription_id", sub.getId());
                row.put("source_name", (sub.getSource() != null) ? sub.getSource().getName() : "N/A");
                row.put("hop_count", sub.getHops());
                row.put("subscription_region", (sub.getRegion() != null) ? sub.getRegion().toShortString() : "N/A");
         
                List<String> pathNames = sub.getBrokerPath();
                List<String> pathRegions = sub.getBrokerRegionPath();
                List<String> combinedPath = new ArrayList<>();

                // Zip the two lists together
                int size = Math.min(pathNames.size(), pathRegions.size());
                for (int k = 0; k < size; k++) {
                    combinedPath.add(pathNames.get(k) + " " + pathRegions.get(k));
                }

                // Join with arrow for readability
                row.put("broker_path", String.join(" -> ", combinedPath));

                subscriptionPathData.add(row);
            }
        }
        
        // --- 3. Now, print ALL metrics (base metrics already printed by super) ---
        
        logger.info("\n--- Final Subscription Path Metrics ---");
        logger.info(String.format("  ... Processed %d subscription paths for CSV output.", finalSubscriptionHops.size()));
        printStats("Final Subscription Hops (Network Load)", finalSubscriptionHops);

        logger.info("\n--- Region-Specific Delivery Metrics ---");
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

        // --- 4. Call the parent's CSV writer ONCE with ALL data ---
        if (enableCsvOutput) {
            logger.info("\n--- Writing all metrics to CSV (Run ID: " + this.simulationTimestamp + ") ---");
            // Call the parent's protected method
            writeMetricsToCsv(
                finalSubscriptionHops,        // Child's data
                subscriptionPathData,         // Child's data
                finalPublicationHops,         // Parent's data (from protected field)
                allPublicationProcessingCosts, // Parent's data (from protected field)
                allDeliveredPubHops           // Parent's data (from protected field)
            );
        }
    }
}