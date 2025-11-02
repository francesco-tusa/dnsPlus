package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger; // Import Logger
import simulator.core.Location;
import simulator.core.SimulationRunner;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.population.DataCenterPublishersPlacement; // Import the correct class
import simulator.population.ProportionalSubscribersPlacement;
import simulator.population.TopologyPopulator;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CustomLogger; // Import CustomLogger

/**
 * A generic and configurable class for running a service replication performance simulation.
 *
 * @param <C> The specific type of TopologyConfiguration for the simulation.
 * @param <F> The specific type of AbstractTopologyFactory that uses the configuration C.
 */
public class ConfigurablePerformanceSimulation<
    C extends TopologyConfiguration,
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends SimulationRunner<C, BrokerWithRegion, F> {

    private static final Logger logger = CustomLogger.getLogger(ConfigurablePerformanceSimulation.class.getName()); // Get logger

    // --- Simulation Parameters ---
    private final long totalSubscribers;
    private final int numberOfReplicas;
    private final double subscriptionRegionSize;
    private final double remoteInterestProbability;

    // --- Data Collection ---
    protected final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    protected final List<PublisherWithLocation> allPublishers = new ArrayList<>();
    protected final Random random = new Random();

    public ConfigurablePerformanceSimulation(long totalSubscribers, int numberOfReplicas, double subscriptionRegionSize, double remoteInterestProbability) {
        this.totalSubscribers = totalSubscribers;
        this.numberOfReplicas = numberOfReplicas;
        this.subscriptionRegionSize = subscriptionRegionSize;
        this.remoteInterestProbability = remoteInterestProbability;
    }

    /**
     * Overrides the abstract method from the parent class.
     * Performance tests should be quiet to avoid I/O overhead.
     * @return The INFO log level.
     */
    @Override
    protected Level getLogLevel() {
        return Level.INFO;
    }

    @Override
    protected void setupSimulation() {
        logger.info("\n--- Populating Topology for Performance Simulation ---");
        if (this.rootNode == null) {
            logger.severe("Cannot populate topology: Root node is null.");
            return;
        }

        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        if (leafBrokers.isEmpty()) {
            logger.severe("Error: No leaf brokers found. Cannot attach clients.");
            return;
        }

        TopologyPopulator populater = new TopologyPopulator(
            new ProportionalSubscribersPlacement(), 
            new DataCenterPublishersPlacement(30) // Provide 30 as the default maxDataCenters
        );
        
        populater.populate(this.rootNode, leafBrokers, totalSubscribers, numberOfReplicas);
        
        collectClients(leafBrokers);
    }

    @Override
    protected void executeScenarios() {
        logger.info("\n--- Executing Performance Scenario with Replicas ---");

        if (allSubscribers.isEmpty()) {
            logger.warning("No subscribers were created. Cannot run scenarios.");
            return;
        }

        if (allPublishers.isEmpty()) {
            logger.warning("No publishers (replicas) were placed. Aborting.");
            return;
        }

        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        
        logger.info("\n>>> Phase 1: Subscribers are sending subscriptions... <<<");
        for (SubscriberWithLocation subscriber : allSubscribers) {
            SubscriptionWithRegion subscription = generateSubscriptionForSubscriber(subscriber, leafBrokers);
            subscriber.send(subscription);
        }

        logger.info("\n>>> Phase 2: All service replicas are sending their publications... <<<");
        for(PublisherWithLocation publisher : allPublishers) {
            publisher.send(new simulator.events.PublicationWithLocation(publisher.getLocation()));
        }
        
        collectAndPrintMetrics();
    }
    
    protected SubscriptionWithRegion generateSubscriptionForSubscriber(SubscriberWithLocation subscriber, List<BrokerWithRegion> allLeafBrokers) {
        Location centerOfInterest;

        if (random.nextDouble() < remoteInterestProbability) {
            BrokerWithRegion remoteBroker = allLeafBrokers.get(random.nextInt(allLeafBrokers.size()));
            centerOfInterest = getRandomLocationInRegion(remoteBroker.getRegion());
        } else {
            centerOfInterest = subscriber.getLocation();
        }

        Region subscriptionRegion = new Region(
            new Location(centerOfInterest.getX() - (subscriptionRegionSize / 2), 
                         centerOfInterest.getY() - (subscriptionRegionSize / 2), 0),
            new Location(centerOfInterest.getX() + (subscriptionRegionSize / 2), 
                         centerOfInterest.getY() + (subscriptionRegionSize / 2), 0)
        );
        
        return new SubscriptionWithRegion(subscriptionRegion);
    }

    private void collectClients(List<BrokerWithRegion> leafBrokers) {
        allSubscribers.clear();
        allPublishers.clear();
        for (BrokerWithRegion leaf : leafBrokers) {
            for (Object child : leaf.getChildren()) {
                if (child instanceof SubscriberWithLocation) {
                    allSubscribers.add((SubscriberWithLocation) child);
                } else if (child instanceof PublisherWithLocation) {
                    allPublishers.add((PublisherWithLocation) child);
                }
            }
        }
        logger.info("Collected " + allSubscribers.size() + " subscribers and " + allPublishers.size() + " publishers.");
    }
    
    protected List<BrokerWithRegion> findLeafBrokers(BrokerWithRegion root) {
        List<BrokerWithRegion> leaves = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);
        
        while(!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof BrokerWithRegion) {
                boolean hasBrokerChild = false;
                for (TreeNode child : current.getChildren()) {
                    if (child instanceof BrokerWithRegion) {
                        hasBrokerChild = true;
                        break;
                    }
                }
                if (!hasBrokerChild) {
                    leaves.add((BrokerWithRegion) current);
                }
            }
            if (current.getChildren() != null) queue.addAll(current.getChildren());
        }
        return leaves;
    }
    
    private void collectAndPrintMetrics() {
        logger.info("\n--- Simulation Metrics ---");
        long totalSubscriptionTableEntries = 0, totalRegionUpdates = 0;
        long successfulNotifications = 0, totalPublicationsSent = 0;

        List<SimulationBroker> allBrokers = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        if (this.rootNode != null) queue.add(this.rootNode);
        
        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof SimulationBroker) allBrokers.add((SimulationBroker) current);
            if (current.getChildren() != null) queue.addAll(current.getChildren());
        }

        for (SimulationBroker broker : allBrokers) {
            totalSubscriptionTableEntries += broker.getSubscriptionsTable().size();
            if (broker instanceof BrokerWithRegion) totalRegionUpdates += ((BrokerWithRegion) broker).getNumOfRegionUpdates();
        }
        for (SubscriberWithLocation subscriber : allSubscribers) successfulNotifications += subscriber.getnPublications();
        for (PublisherWithLocation publisher : allPublishers) totalPublicationsSent += publisher.getnPublications();

        logger.info("--- System Overhead Metrics ---");
        logger.info("Total Subscription Table Entries Created (Propagation Cost): " + totalSubscriptionTableEntries);
        logger.info("Total Region Boundary Updates: " + totalRegionUpdates);
        logger.info("\n--- Service Delivery Metrics ---");
        logger.info("Total Publications Sent by all Replicas: " + totalPublicationsSent);
        logger.info("Total Successful Notifications Received by Subscribers: " + successfulNotifications);
        
        if (totalSubscribers > 0) {
            double matchRate = (double) successfulNotifications / totalSubscribers * 100.0;
            logger.info(String.format("Subscriber Match Rate: %.2f%%", matchRate));
        }
    }

    protected Location getRandomLocationInRegion(Region region) {
        if (region == null) {
            return new Location(0, 0, 0); // Fallback
        }
        Random rand = new Random();
        double x = region.getBottomLeft().getX() + (region.getTopRight().getX() - region.getBottomLeft().getX()) * rand.nextDouble();
        double y = region.getBottomLeft().getY() + (region.getTopRight().getY() - region.getBottomLeft().getY()) * rand.nextDouble();
        return new Location(x, y, 0);
    }
}