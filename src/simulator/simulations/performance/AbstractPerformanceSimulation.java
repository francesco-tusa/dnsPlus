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
import simulator.core.TreeNode; // Import TreeNode
import simulator.entities.PublisherWithLocation;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.population.DataCenterPublishersPlacement;
import simulator.population.ProportionalSubscribersPlacement;
import simulator.population.TopologyPopulator;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region; // Import Region
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public abstract class AbstractPerformanceSimulation<
    C extends TopologyConfiguration, 
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends SimulationRunner<C, BrokerWithRegion, F> {

    // --- NEW: Add a logger instance to this class ---
    private static final Logger logger = CustomLogger.getLogger(AbstractPerformanceSimulation.class.getName());

    protected final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    protected final List<PublisherWithLocation> allPublishers = new ArrayList<>();
    protected final Random random = new Random();

    protected final int numberOfReplicas;
    protected final int subscribersPerReplica;
    protected final long totalSubscribers;
    protected final boolean enableCsvOutput;

    public AbstractPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica, boolean enableCsvOutput) {
        if (numberOfReplicas <= 0) throw new IllegalArgumentException("Number of replicas must be positive.");
        if (subscribersPerReplica < 0) throw new IllegalArgumentException("Subscribers per replica cannot be negative.");
        
        this.numberOfReplicas = numberOfReplicas;
        this.subscribersPerReplica = subscribersPerReplica;
        this.totalSubscribers = (long) numberOfReplicas * subscribersPerReplica;
        this.enableCsvOutput = enableCsvOutput;
    }
    
    public AbstractPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        this(numberOfReplicas, subscribersPerReplica, false); 
    }

    protected long getTotalSubscribers() { return totalSubscribers; }
    protected int getNumberOfReplicas() { return numberOfReplicas; }

    @Override
    protected Level getLogLevel() {
        return Level.INFO;
    }

    @Override
    protected void setupSimulation() {
        System.out.println("\n--- Populating Topology for Performance Simulation ---");
        System.out.printf("Setup: %d Replicas, %d Subscribers/Replica (Total Subscribers: %d)%n", 
                          numberOfReplicas, subscribersPerReplica, totalSubscribers);

        if (this.rootNode == null) {
            System.err.println("Cannot populate topology: Root node is null.");
            return;
        }

        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        if (leafBrokers.isEmpty()) {
            System.err.println("Error: No leaf brokers found. Cannot attach clients.");
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("--- DEBUG: Final Broker Hierarchy and Regions ---");
            // Call the new helper method, starting from the root
            logBrokerHierarchy(this.rootNode, "  ");
            logger.fine("-------------------------------------------------");
        }
        
        TopologyPopulator populater = new TopologyPopulator(
            new ProportionalSubscribersPlacement(), 
            new DataCenterPublishersPlacement(30)
        );
        populater.populate(this.rootNode, leafBrokers, getTotalSubscribers(), getNumberOfReplicas());
        
        collectClients(leafBrokers);
    }
    
    /**
     * Recursively walks the topology tree and logs the name and region
     * of each broker node.
     * @param node The current node to log.
     * @param indent The indentation string for pretty-printing the tree.
     */
    private void logBrokerHierarchy(TreeNode node, String indent) {
        if (node == null) return;
        
        // Only log nodes that are brokers
        if (node instanceof BrokerWithRegion broker) {
            Region region = broker.getRegion();
            String regionInfo = "N/A";
            
            // Check if region and its points are valid before trying to format
            if (region != null && region.getBottomLeft() != null && region.getTopRight() != null) {
                regionInfo = String.format("Region: %s (W: %.2f, H: %.2f)",
                                            region.toShortString(),
                                            region.getWidth(),
                                            region.getHeight());
            }

            logger.fine(String.format("%s%s [%s]", indent, broker.getName(), regionInfo));

            // Recurse for all children
            for (TreeNode child : broker.getChildren()) {
                logBrokerHierarchy(child, indent + "  ");
            }
        }
        // We stop recursing if the node is not a broker 
        // (e.g., if it's a SubscriberWithLocation)
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
        
        List<Integer> allSubscriptionHops = new ArrayList<>();
        List<Integer> allPublicationHops = new ArrayList<>();
        List<Integer> allDeliveredPubHops = new ArrayList<>();
        List<Long> allPublicationProcessingCosts = new ArrayList<>();

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
            
            allSubscriptionHops.addAll(broker.getAllProcessedSubscriptionHops());
            allPublicationHops.addAll(broker.getAllProcessedPublicationHops());
            allPublicationProcessingCosts.addAll(broker.getPublicationProcessingCosts());
        }
        
        for (SubscriberWithLocation subscriber : allSubscribers) {
            successfulNotifications += subscriber.getnPublications();
            allDeliveredPubHops.addAll(subscriber.getReceivedPublicationHops());
        }
        for (PublisherWithLocation publisher : allPublishers) {
            totalPublicationsSent += publisher.getnPublications();
        }

        System.out.println("--- System Overhead Metrics ---");
        System.out.println("Total Subscription Table Entries Created (Storage Cost): " + totalSubscriptionTableEntries);
        System.out.println("Total Region Boundary Updates: " + totalRegionUpdates);
        printStats("All Subscription Hops (Network Load)", allSubscriptionHops);
        printStats("All Publication Hops (Network Load)", allPublicationHops);
        printStatsLong("Publication Processing Cost (CPU Load)", allPublicationProcessingCosts);
        
        System.out.println("\n--- Service Delivery Metrics ---");
        System.out.println("Total Publications Sent by all Replicas: " + totalPublicationsSent);
        System.out.println("Total Successful Notifications Received by Subscribers: " + successfulNotifications);
        printStats("Delivered Publication Hops (Path Length)", allDeliveredPubHops);
        
        if (enableCsvOutput) {
            writeMetricsToCsv(allSubscriptionHops, allPublicationHops, allPublicationProcessingCosts, allDeliveredPubHops);
        }
    }
    
    private void writeMetricsToCsv(List<Integer> subHops, List<Integer> pubHops, List<Long> pubCosts, List<Integer> deliveredHops) {
        String timestamp = this.simulationTimestamp; 
        String outputDir = "output/metrics/";
        System.out.println("\n--- Writing raw metrics to CSV files (Run ID: " + timestamp + ") ---");
        
        CsvMetricWriter.writeListToCsv(
            outputDir + timestamp + "_subscription_hops.csv", 
            "hop_count", 
            subHops);
            
        CsvMetricWriter.writeListToCsv(
            outputDir + timestamp + "_publication_hops.csv", 
            "hop_count", 
            pubHops);
            
        CsvMetricWriter.writeListToCsv(
            outputDir + timestamp + "_publication_processing_cost.csv", 
            "processing_cost", 
            pubCosts);
            
        CsvMetricWriter.writeListToCsv(
            outputDir + timestamp + "_delivered_publication_hops.csv", 
            "hop_count", 
            deliveredHops);
    }
    
    protected void printStats(String name, List<Integer> data) {
        if (data == null || data.isEmpty()) {
            System.out.printf("%s: N/A (no data)%n", name);
            return;
        }
        double sum = 0;
        for (int val : data) {
            sum += val;
        }
        double mean = sum / data.size();

        double sumSqDiff = 0;
        for (int val : data) {
            sumSqDiff += (val - mean) * (val - mean);
        }
        double variance = sumSqDiff / data.size();
        double stdDev = Math.sqrt(variance); 

        System.out.printf("%s: Avg=%.2f, StdDev=%.2f, Variance=%.2f (N=%d)%n", 
                          name, mean, stdDev, variance, data.size());
    }
    
    protected void printStatsLong(String name, List<Long> data) {
        if (data == null || data.isEmpty()) {
            System.out.printf("%s: N/A (no data)%n", name);
            return;
        }
        double sum = 0;
        for (long val : data) {
            sum += val;
        }
        double mean = sum / data.size();

        double sumSqDiff = 0;
        for (long val : data) {
            sumSqDiff += (val - mean) * (val - mean);
        }
        double variance = sumSqDiff / data.size();
        double stdDev = Math.sqrt(variance); 

        System.out.printf("%s: Avg=%.2f, StdDev=%.2f, Variance=%.2f (N=%d)%n", 
                          name, mean, stdDev, variance, data.size());
    }


    protected Location getRandomLocationInRegion(Region region) {
        Random rand = new Random();
        if (region == null || region.getBottomLeft() == null || region.getTopRight() == null) {
            System.err.println("Warning: Attempted to get random location in null or incomplete region.");
            return new Location(0, 0, 0);
        }
        double minX = region.getBottomLeft().getX();
        double maxX = region.getTopRight().getX();
        double minY = region.getBottomLeft().getY();
        double maxY = region.getTopRight().getY();
        double rangeX = maxX - minX;
        double rangeY = maxY - minY;
        
        double x = minX + (rangeX > 0 ? rand.nextDouble() * rangeX : 0);
        double y = minY + (rangeY > 0 ? rand.nextDouble() * rangeY : 0);
        
        return new Location(x, y, 0);
    }
}
