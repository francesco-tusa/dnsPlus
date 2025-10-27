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
        
        // New metric lists - These now collect from ALL brokers
        List<Integer> allSubscriptionHops = new ArrayList<>();
        List<Integer> allPublicationHops = new ArrayList<>(); // Correctly aggregated now
        
        // Metric list for delivered publications (collected from subscribers)
        List<Integer> allDeliveredPubHops = new ArrayList<>();

        List<SimulationBroker> allBrokers = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        if (this.rootNode != null) queue.add(this.rootNode);
        
        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof SimulationBroker) allBrokers.add((SimulationBroker) current);
            if (current.getChildren() != null) queue.addAll(current.getChildren());
        }

        // Aggregate hop counts from ALL brokers
        for (SimulationBroker broker : allBrokers) {
            totalSubscriptionTableEntries += broker.getSubscriptionsTable().size();
            if (broker instanceof BrokerWithRegion) totalRegionUpdates += ((BrokerWithRegion) broker).getNumOfRegionUpdates();
            
            // Aggregate the hop distributions from all brokers
            allSubscriptionHops.addAll(broker.getAllProcessedSubscriptionHops());
            allPublicationHops.addAll(broker.getAllProcessedPublicationHops()); // Correctly aggregating now
        }
        
        // Aggregate delivered publication details from subscribers
        for (SubscriberWithLocation subscriber : allSubscribers) {
            successfulNotifications += subscriber.getnPublications();
            allDeliveredPubHops.addAll(subscriber.getReceivedPublicationHops());
        }
        // Count total publications sent
        for (PublisherWithLocation publisher : allPublishers) {
            totalPublicationsSent += publisher.getnPublications();
        }

        System.out.println("--- System Overhead Metrics ---");
        System.out.println("Total Subscription Table Entries Created (Propagation Cost): " + totalSubscriptionTableEntries);
        System.out.println("Total Region Boundary Updates: " + totalRegionUpdates);
        // Print new hop stats for *all* network traversal events
        printStats("All Subscription Hops (Network Load)", allSubscriptionHops);
        printStats("All Publication Hops (Network Load)", allPublicationHops); // Correctly printing stats
        
        System.out.println("\n--- Service Delivery Metrics ---");
        System.out.println("Total Publications Sent by all Replicas: " + totalPublicationsSent);
        System.out.println("Total Successful Notifications Received by Subscribers: " + successfulNotifications);
        // Print stats specifically for *delivered* publications (path length to subscriber)
        printStats("Delivered Publication Hops (Path Length)", allDeliveredPubHops);
    }
    
    /**
     * Helper method to calculate and print statistics for a list of numbers.
     * Includes Mean (Avg), Standard Deviation, Variance (Mean Squared Error relative to Mean), and N.
     * @param name The name of the metric.
     * @param data A list of integer data points.
     */
    protected void printStats(String name, List<Integer> data) {
        if (data == null || data.isEmpty()) {
            System.out.printf("%s: N/A (no data)%n", name);
            return;
        }
        // Use double for sum to avoid overflow on large N
        double sum = 0;
        for (int val : data) {
            sum += val;
        }
        double mean = sum / data.size();

        double sumSqDiff = 0;
        for (int val : data) {
            sumSqDiff += (val - mean) * (val - mean);
        }
        // Variance (Mean Squared Error relative to the mean)
        // Use data.size() for population variance, which is fine for large N
        double variance = sumSqDiff / data.size(); 
        double stdDev = Math.sqrt(variance); 

        // Added Variance (Mean Squared Error) to the output
        System.out.printf("%s: Avg=%.2f, StdDev=%.2f, Variance=%.2f (N=%d)%n", 
                          name, mean, stdDev, variance, data.size());
    }

    protected Location getRandomLocationInRegion(Region region) {
        Random rand = new Random();
        // Ensure bottomLeft and topRight are not null before accessing coordinates
        if (region == null || region.getBottomLeft() == null || region.getTopRight() == null) {
            // Handle error or return a default location
            System.err.println("Warning: Attempted to get random location in null or incomplete region.");
            return new Location(0, 0, 0); // Default location
        }
        double minX = region.getBottomLeft().getX();
        double maxX = region.getTopRight().getX();
        double minY = region.getBottomLeft().getY();
        double maxY = region.getTopRight().getY();
        
        // Basic handling for non-wrapping case. Needs adjustment if regions can wrap.
        double rangeX = maxX - minX;
        double rangeY = maxY - minY;

        double x = minX + (rangeX > 0 ? rand.nextDouble() * rangeX : 0);
        double y = minY + (rangeY > 0 ? rand.nextDouble() * rangeY : 0);
        
        // Assuming Z is 0 for performance simulations based on previous context
        return new Location(x, y, 0);
    }
}