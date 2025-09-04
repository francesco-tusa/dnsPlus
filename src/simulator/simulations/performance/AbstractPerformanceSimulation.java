package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import simulator.core.Location;
import simulator.core.SimulationRunner;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.population.HubPublishersPlacement;
import simulator.population.ProportionalSubscribersPlacement;
import simulator.population.TopologyPopulator;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;

/**
 * An abstract base class for running performance simulations.
 * It uses a TopologyPopulator to handle client setup.
 */
public abstract class AbstractPerformanceSimulation<
    C extends TopologyConfiguration, 
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends SimulationRunner<C, BrokerWithRegion, F> {

    protected final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    protected final List<PublisherWithLocation> allPublishers = new ArrayList<>();
    protected final Random random = new Random();

    // --- Abstract Methods for Subclasses ---
    protected abstract long getTotalSubscribers();
    protected abstract int getNumberOfReplicas();
    protected abstract double getRemoteInterestProbability();

    @Override
    protected Level getLogLevel() {
        return Level.INFO;
    }

    @Override
    protected void setupSimulation() {
        System.out.println("\n--- Populating Topology for Performance Simulation ---");
        if (this.rootNode == null) {
            System.err.println("Cannot populate topology: Root node is null.");
            return;
        }

        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        if (leafBrokers.isEmpty()) {
            System.err.println("Error: No leaf brokers found. Cannot attach clients.");
            return;
        }

        TopologyPopulator populater = new TopologyPopulator(
            new ProportionalSubscribersPlacement(), 
            new HubPublishersPlacement()
        );
        populater.populate(this.rootNode, leafBrokers, getTotalSubscribers(), getNumberOfReplicas());
        
        collectClients(leafBrokers);
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
    
    protected void collectAndPrintMetrics() {
        System.out.println("\n--- Simulation Metrics ---");
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

        System.out.println("--- System Overhead Metrics ---");
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

    protected Location getRandomLocationInRegion(Region region) {
        Random rand = new Random();
        double x = region.getBottomLeft().getX() + (region.getTopRight().getX() - region.getBottomLeft().getX()) * rand.nextDouble();
        double y = region.getBottomLeft().getY() + (region.getTopRight().getY() - region.getBottomLeft().getY()) * rand.nextDouble();
        return new Location(x, y, 0);
    }
}