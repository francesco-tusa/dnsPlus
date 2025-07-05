package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;

import simulator.Location;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.SimulationBroker;
import simulator.SimulationRunner;
import simulator.TreeNode;
import simulator.clients.ProportionalSubscriberGenerator;
import simulator.regions.BrokerWithRegion;
import simulator.regions.BrokerWithRegionProcessingLocation;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * An abstract base class for running service replication simulations.
 * It contains the common logic for setting up clients, executing scenarios,
 * and gathering metrics, while allowing subclasses to define the specific
 * topology and client behaviors to be used.
 *
 * @param <C> The specific type of TopologyConfiguration.
 * @param <F> The specific type of AbstractTopologyFactory.
 */
public abstract class AbstractServiceSimulation<
    C extends TopologyConfiguration, 
    F extends AbstractTopologyFactory<C, BrokerWithRegionProcessingLocation>
> extends SimulationRunner<C, BrokerWithRegionProcessingLocation, F> {

    // --- Data Collection ---
    protected final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    protected final List<PublisherWithLocation> allPublishers = new ArrayList<>();
    protected final Random random = new Random();

    // --- Abstract Methods for Subclasses ---

    protected abstract long getTotalSubscribers();
    protected abstract int getNumberOfReplicas();
    protected abstract double getSubscriptionRegionSize();
    
    /**
     * Subclasses must implement this method to define the probability that a
     * subscriber will be interested in a remote region vs. their local region.
     * @return A value between 0.0 and 1.0 representing the probability.
     */
    protected abstract double getRemoteInterestProbability();

    /**
     * Subclasses must implement this method to define the logic for how a
     * subscriber creates their region of interest. This allows for modeling
     * different user behaviors (e.g., local vs. remote interest).
     *
     * @param subscriber The subscriber for whom to generate a subscription.
     * @param allLeafBrokers The list of all leaf brokers in the topology, for context.
     * @return The SubscriptionWithRegion object for the subscriber.
     */
    protected abstract SubscriptionWithRegion generateSubscriptionForSubscriber(SubscriberWithLocation subscriber, List<LeafBrokerWithRegionProcessingRegion> allLeafBrokers);


    // --- Common Simulation Logic ---

    @Override
    protected void setupSimulation() {
        System.out.println("\n--- Populating World for a Single Service Simulation ---");
        if (this.rootNode == null) {
            System.err.println("Cannot populate world: Root node is null.");
            return;
        }

        List<LeafBrokerWithRegionProcessingRegion> leafBrokers = findLeafBrokers(this.rootNode);
        if (leafBrokers.isEmpty()) {
            System.err.println("Error: No leaf brokers found. Cannot attach clients.");
            return;
        }

        ProportionalSubscriberGenerator subscriberGenerator = new ProportionalSubscriberGenerator();
        subscriberGenerator.generateAndAttach(this.rootNode, leafBrokers, getTotalSubscribers());
        
        collectSubscribers(leafBrokers);
    }

    @Override
    protected void executeScenarios() {
        System.out.println("\n--- Executing Single Service Scenario with Replicas ---");

        if (allSubscribers.isEmpty()) {
            System.err.println("No subscribers were created. Cannot run scenarios.");
            return;
        }

        placeServiceReplicas();
        if (allPublishers.isEmpty()) {
            System.err.println("No publishers (replicas) were placed. Aborting.");
            return;
        }

        List<LeafBrokerWithRegionProcessingRegion> leafBrokers = findLeafBrokers(this.rootNode);
        
        System.out.println("\n>>> Phase 1: Subscribers are sending subscriptions... <<<");
        for (SubscriberWithLocation subscriber : allSubscribers) {
            // The logic for creating the subscription is now delegated to a configurable method.
            SubscriptionWithRegion subscription = generateSubscriptionForSubscriber(subscriber, leafBrokers);
            subscriber.send(subscription);
        }

        System.out.println("\n>>> Phase 2: All service replicas are sending their publications... <<<");
        for(PublisherWithLocation publisher : allPublishers) {
            publisher.send(new simulator.PublicationWithLocation(publisher.getLocation()));
        }
        
        collectAndPrintMetrics();
    }

    private void placeServiceReplicas() {
        System.out.println("\n>>> Placing " + getNumberOfReplicas() + " service replicas... <<<");
        allPublishers.clear();
        List<LeafBrokerWithRegionProcessingRegion> leafBrokers = findLeafBrokers(this.rootNode);

        List<LeafBrokerWithRegionProcessingRegion> sortedHubs = leafBrokers.stream()
            .sorted(Comparator.comparingLong(BrokerWithRegion::getInternetPopulation).reversed())
            .collect(Collectors.toList());

        int hubCount = Math.min(getNumberOfReplicas(), sortedHubs.size());
        for (int i = 0; i < hubCount; i++) {
            LeafBrokerWithRegionProcessingRegion hub = sortedHubs.get(i);
            Location pubLocation = getRandomLocationInRegion(hub.getRegion());
            PublisherWithLocation replica = new PublisherWithLocation("ServiceReplica-" + i, pubLocation);
            hub.addChild(replica);
            allPublishers.add(replica);
            System.out.println("Placed " + replica.getName() + " in hub " + hub.getName() + " (Pop: " + hub.getInternetPopulation() + ")");
        }
    }

    private void collectSubscribers(List<LeafBrokerWithRegionProcessingRegion> leafBrokers) {
        allSubscribers.clear();
        for (LeafBrokerWithRegionProcessingRegion leaf : leafBrokers) {
            for (Object child : leaf.getChildren()) {
                if (child instanceof SubscriberWithLocation) {
                    allSubscribers.add((SubscriberWithLocation) child);
                }
            }
        }
        System.out.println("Collected " + allSubscribers.size() + " subscribers.");
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
        
        if (getTotalSubscribers() > 0) {
            double matchRate = (double) successfulNotifications / getTotalSubscribers() * 100.0;
            System.out.printf("Subscriber Match Rate: %.2f%%\n", matchRate);
        }
    }
    
    protected List<LeafBrokerWithRegionProcessingRegion> findLeafBrokers(BrokerWithRegionProcessingLocation root) {
        List<LeafBrokerWithRegionProcessingRegion> leaves = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);
        while(!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof LeafBrokerWithRegionProcessingRegion) leaves.add((LeafBrokerWithRegionProcessingRegion) current);
            if (current.getChildren() != null) queue.addAll(current.getChildren());
        }
        return leaves;
    }

    protected Location getRandomLocationInRegion(Region region) {
        Random rand = new Random();
        double x = region.getBottomLeft().getX() + (region.getTopRight().getX() - region.getBottomLeft().getX()) * rand.nextDouble();
        double y = region.getBottomLeft().getY() + (region.getTopRight().getY() - region.getBottomLeft().getY()) * rand.nextDouble();
        return new Location(x, y, 0);
    }
}
