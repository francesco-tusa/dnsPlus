package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import simulator.Location;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.SimulationBroker;
import simulator.SimulationRunner;
import simulator.TreeNode;
import simulator.clients.TopologyPopulater;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * A generic and configurable class for running a service replication performance simulation.
 * It is parameterized to work with a specific configuration (C) and a factory (F) that uses it.
 *
 * @param <C> The specific type of TopologyConfiguration for the simulation.
 * @param <F> The specific type of AbstractTopologyFactory that uses the configuration C.
 */
public class ConfigurablePerformanceSimulation<
    C extends TopologyConfiguration,
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends SimulationRunner<C, BrokerWithRegion, F> {

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

    @Override
    protected void setupSimulation() {
        System.out.println("\n--- Populating World for Performance Simulation ---");
        if (this.rootNode == null) {
            System.err.println("Cannot populate world: Root node is null.");
            return;
        }

        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        if (leafBrokers.isEmpty()) {
            System.err.println("Error: No leaf brokers found. Cannot attach clients.");
            return;
        }

        TopologyPopulater populater = new TopologyPopulater();
        populater.populate(this.rootNode, leafBrokers, totalSubscribers, numberOfReplicas);
        
        collectClients(leafBrokers);
    }

    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Executing Performance Scenario with Replicas ---");

        if (allSubscribers.isEmpty()) {
            System.err.println("No subscribers were created. Cannot run scenarios.");
            return;
        }

        if (allPublishers.isEmpty()) {
            System.err.println("No publishers (replicas) were placed. Aborting.");
            return;
        }

        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        
        System.out.println("\n>>> Phase 1: Subscribers are sending subscriptions... <<<");
        for (SubscriberWithLocation subscriber : allSubscribers) {
            SubscriptionWithRegion subscription = generateSubscriptionForSubscriber(subscriber, leafBrokers);
            subscriber.send(subscription);
        }

        System.out.println("\n>>> Phase 2: All service replicas are sending their publications... <<<");
        for(PublisherWithLocation publisher : allPublishers) {
            publisher.send(new simulator.PublicationWithLocation(publisher.getLocation()));
        }
        
        collectAndPrintMetrics();
    }

    private SubscriptionWithRegion generateSubscriptionForSubscriber(SubscriberWithLocation subscriber, List<BrokerWithRegion> allLeafBrokers) {
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
        System.out.println("Collected " + allSubscribers.size() + " subscribers and " + allPublishers.size() + " publishers.");
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
        System.out.println("\n--- Simulation Metrics ---");
        long totalSubscriptionMessages = 0, totalRegionUpdates = 0, totalSubscriptionTableEntries = 0;
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
            totalSubscriptionMessages += broker.getnSubscriptions();
            totalSubscriptionTableEntries += broker.getSubscriptionsTable().size();
            if (broker instanceof BrokerWithRegion) totalRegionUpdates += ((BrokerWithRegion) broker).getNumOfRegionUpdates();
        }
        for (SubscriberWithLocation subscriber : allSubscribers) successfulNotifications += subscriber.getnPublications();
        for (PublisherWithLocation publisher : allPublishers) totalPublicationsSent += publisher.getnPublications();

        System.out.println("--- System Overhead Metrics ---");
        System.out.println("Total Subscription Messages Processed by Brokers: " + totalSubscriptionMessages);
        System.out.println("Total Subscription Table Entries Created (Propagation Cost): " + totalSubscriptionTableEntries);
        System.out.println("Total Region Boundary Updates: " + totalRegionUpdates);
        System.out.println("\n--- Service Delivery Metrics ---");
        System.out.println("Total Publications Sent by all Replicas: " + totalPublicationsSent);
        System.out.println("Total Successful Notifications Received by Subscribers: " + successfulNotifications);
        
        if (totalSubscribers > 0) {
            double matchRate = (double) successfulNotifications / totalSubscribers * 100.0;
            System.out.printf("Subscriber Match Rate: %.2f%%\n", matchRate);
        }
    }

    protected Location getRandomLocationInRegion(Region region) {
        Random rand = new Random();
        double x = region.getBottomLeft().getX() + (region.getTopRight().getX() - region.getBottomLeft().getX()) * rand.nextDouble();
        double y = region.getBottomLeft().getY() + (region.getTopRight().getY() - region.getBottomLeft().getY()) * rand.nextDouble();
        return new Location(x, y, 0);
    }
}