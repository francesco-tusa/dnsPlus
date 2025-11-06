package simulator.simulations.performance;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger; // Import Logger
import java.util.stream.Collectors;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
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

    @Override
    protected void executeScenarios() {
        logger.info("\n--- Executing Region-Based Performance Scenario ---");
        if (allSubscribers.isEmpty() || allPublishers.isEmpty()) {
            logger.severe("No subscribers or publishers were created. Cannot run scenarios.");
            return;
        }
        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        
        logger.info("\n>>> Phase 1: Subscribers are sending region-based subscriptions... <<<");
        final int PROGRESS_INTERVAL = (int) Math.max(1000, getTotalSubscribers() / 10);
        for (int i = 0; i < allSubscribers.size(); i++) {
            SubscriberWithLocation subscriber = allSubscribers.get(i);
            SubscriptionWithRegion subscription = generateSubscriptionForSubscriber(subscriber, leafBrokers);
            subscriber.send(subscription);
            if ((i + 1) % PROGRESS_INTERVAL == 0 || (i+1) == allSubscribers.size()) {
                logger.info(String.format("  ... processed %d / %d subscriptions.", (i + 1), allSubscribers.size()));
            }
        }

        logger.info("\n>>> Phase 2: All service replicas are sending their publications... <<<");
        for(var publisher : allPublishers) {
            publisher.send(new simulator.events.PublicationWithLocation(publisher.getLocation()));
        }
        
        collectAndPrintMetrics();
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
        super.collectAndPrintMetrics();
        if (getTotalSubscribers() > 0) {
            long matchedSubscribers = allSubscribers.stream().filter(s -> s.getnPublications() > 0).count();
            double matchRate = (double) matchedSubscribers / getTotalSubscribers() * 100.0;
            logger.info(String.format("Subscriber Match Rate: %.2f%% (%d / %d)", 
                                      matchRate, matchedSubscribers, getTotalSubscribers()));
        }
    }
}