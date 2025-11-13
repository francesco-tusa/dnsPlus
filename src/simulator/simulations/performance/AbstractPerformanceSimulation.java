package simulator.simulations.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.SimulationRunner;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.TrackableEvent;
import simulator.population.DataCenterPublishersPlacement;
import simulator.population.ProportionalSubscribersPlacement;
import simulator.population.TopologyPopulator;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import simulator.visualisation.SimulationVisualiser;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public abstract class AbstractPerformanceSimulation<
    C extends TopologyConfiguration, 
    F extends AbstractTopologyFactory<C, BrokerWithRegion>
> extends SimulationRunner<C, BrokerWithRegion, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractPerformanceSimulation.class.getName());

    protected final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    protected final List<PublisherWithLocation> allPublishers = new ArrayList<>();
    protected final Random random = new Random();

    protected final int numberOfReplicas;
    protected final int subscribersPerReplica;
    protected final long totalSubscribers;
    protected long successfulNotifications = 0;
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
        logger.info("\n--- Populating Topology for Performance Simulation ---");
        
        // Logs client counts (e.g., 20 Replicas, 2500 Subscribers/Replica)
        logger.info(String.format("Client Setup: %d Replicas, %d Subscribers/Replica (Total Subscribers: %d)", 
                                  numberOfReplicas, subscribersPerReplica, totalSubscribers));

        // Logs topology info (e.g., File: output/geonames_topology.json)
        if (this.topologyConfig instanceof RegionRandomTopologyConfiguration config) {
            logger.info(String.format(
                "Topology Setup (Random): Depth=%d, MaxBranch=%d, NumRegions=%d, WorldSize=[%.1f x %.1f]",
                config.getTreeDepth(),
                config.getMaxBranchingFactor(),
                config.getNumRegions(),
                config.getWorldWidth(),
                config.getWorldHeight()
            ));
        } else if (this.topologyConfig instanceof FileBasedTopologyConfiguration config) {
            logger.info(String.format("Topology Setup (File): %s", config.getTopologyFilePath()));
        }

        if (this.rootNode == null) {
            logger.severe("Cannot populate topology: Root node is null.");
            return;
        }

        logTopologySummary(this.rootNode);

        List<BrokerWithRegion> leafBrokers = findLeafBrokers(this.rootNode);
        if (leafBrokers.isEmpty()) {
            logger.severe("Error: No leaf brokers found. Cannot attach clients.");
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("--- DEBUG: Final Broker Hierarchy and Regions ---");
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
        
        if (node instanceof BrokerWithRegion broker) {
            Region region = broker.getRegion();
            String regionInfo = "N/A";
            
            if (region != null && region.getBottomLeft() != null && region.getTopRight() != null) {
                regionInfo = String.format("Region: %s (W: %.2f, H: %.2f)",
                                            region.toShortString(),
                                            region.getWidth(),
                                            region.getHeight());
            }

            logger.fine(String.format("%s%s [%s]", indent, broker.getName(), regionInfo));

            for (TreeNode child : broker.getChildren()) {
                logBrokerHierarchy(child, indent + "  ");
            }
        }
    }


/**
     * Helper class for the Plane Sweep algorithm.
     */
    private static class SweepEvent implements Comparable<SweepEvent> {
        enum EventType { START, END }
        
        final double x;
        final EventType type;
        final BrokerWithRegion broker;

        SweepEvent(double x, EventType type, BrokerWithRegion broker) {
            this.x = x;
            this.type = type;
            this.broker = broker;
        }

        @Override
        public int compareTo(SweepEvent other) {
            // Compare by x-coordinate
            int xCompare = Double.compare(this.x, other.x);
            if (xCompare != 0) {
                return xCompare;
            }
            // If x is equal, START events come before END events
            return this.type.compareTo(other.type);
        }
    }

    /**
     * Calculates overlapping sibling pairs using an efficient O(n log n) Plane Sweep algorithm.
     * @param brokers The list of brokers at a single level.
     * @return The total count of overlapping pairs.
     */
    private long calculateSiblingOverlaps(List<BrokerWithRegion> brokers) {
        List<SweepEvent> events = new ArrayList<>(brokers.size() * 2);
        
        // 1. Create all START and END events
        for (BrokerWithRegion broker : brokers) {
            Region region = broker.getRegion();
            if (region == null || region.getBottomLeft() == null || region.getTopRight() == null) {
                continue; // Skip brokers with no valid region
            }

            double minLon = region.getBottomLeft().getX();
            double maxLon = region.getTopRight().getX();

            if (minLon <= maxLon) {
                // Standard case: No wrap-around
                events.add(new SweepEvent(minLon, SweepEvent.EventType.START, broker));
                events.add(new SweepEvent(maxLon, SweepEvent.EventType.END, broker));
            } else {
                // Wrap-around case: Treat as two separate intervals
                // [minLon, 180] and [-180, maxLon]
                events.add(new SweepEvent(minLon, SweepEvent.EventType.START, broker));
                events.add(new SweepEvent(180.0, SweepEvent.EventType.END, broker));
                
                events.add(new SweepEvent(-180.0, SweepEvent.EventType.START, broker));
                events.add(new SweepEvent(maxLon, SweepEvent.EventType.END, broker));
            }
        }

        // 2. Sort the events
        Collections.sort(events);

        // 3. Sweep the line
        long overlapCount = 0;
        // This map tracks active brokers and how many of their "segments" are active
        Map<BrokerWithRegion, Integer> activeSegments = new HashMap<>();
        // This set tracks unique pairs we've already counted to avoid double-counting
        Set<String> countedPairs = new HashSet<>(); 

        for (SweepEvent event : events) {
            BrokerWithRegion eventBroker = event.broker;
            Region r1 = eventBroker.getRegion();

            if (event.type == SweepEvent.EventType.START) {
                // A new region segment starts. Check it against all *other* active brokers.
                for (BrokerWithRegion activeBroker : activeSegments.keySet()) {
                    if (activeBroker == eventBroker) {
                        continue; // Don't check against self
                    }
                    
                    // Create a unique key for this pair
                    String pairKey = (eventBroker.getName().compareTo(activeBroker.getName()) < 0)
                                     ? eventBroker.getName() + "::" + activeBroker.getName()
                                     : activeBroker.getName() + "::" + eventBroker.getName();

                    if (countedPairs.contains(pairKey)) {
                        continue; // Already counted this pair
                    }

                    // Check for Y-axis overlap (altitude/Z is ignored)
                    Region r2 = activeBroker.getRegion();
                    if (r1.intersects(r2)) {
                        overlapCount++;
                        countedPairs.add(pairKey);
                    }
                }
                // Add this segment to the active map
                activeSegments.put(eventBroker, activeSegments.getOrDefault(eventBroker, 0) + 1);

            } else { // event.type == END
                // A region segment ends. Decrement its count.
                int count = activeSegments.getOrDefault(eventBroker, 0);
                if (count <= 1) {
                    activeSegments.remove(eventBroker); // Last segment ended
                } else {
                    activeSegments.put(eventBroker, count - 1);
                }
            }
        }
        return overlapCount;
    }


    /**
     * Traverses the broker topology and logs a summary of its structure,
     * including broker count, average fan-out, and sibling region overlap
     * per level, considering only broker-to-broker connections.
     * Also sends Level 1 regions to the SimulationVisualizer.
     *
     * @param root The root broker of the topology.
     */
    protected void logTopologySummary(BrokerWithRegion root) {
        if (root == null) {
            return;
        }

        logger.info("\n--- Broker Topology Structure Summary ---");
        
        Queue<BrokerWithRegion> queue = new LinkedList<>();
        queue.add(root);

        int currentLevel = 0;
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size(); 
            long totalBrokerChildrenAtLevel = 0; 
            
            List<BrokerWithRegion> brokersAtThisLevel = new ArrayList<>(levelSize);

            for (int i = 0; i < levelSize; i++) {
                BrokerWithRegion broker = queue.poll();
                if (broker == null) continue;
                brokersAtThisLevel.add(broker); 

                if (broker.getChildren() != null) {
                    for (TreeNode child : broker.getChildren()) {
                        if (child instanceof BrokerWithRegion childBroker) { 
                            totalBrokerChildrenAtLevel++;
                            queue.add(childBroker); 
                        }
                    }
                }
            }
            
            if (levelSize > 0) {
                // Log basic stats
                double avgFanOut = (totalBrokerChildrenAtLevel > 0) ? (double) totalBrokerChildrenAtLevel / levelSize : 0.0;
                logger.info(String.format(
                    "  Level %d: %d brokers, Avg. Fan-Out: %.2f",
                    currentLevel,
                    levelSize,
                    avgFanOut
                ));

                // Run efficient overlap calculation
                long totalPossiblePairs = (long) levelSize * (levelSize - 1) / 2;
                if (totalPossiblePairs > 0) {
                    long overlappingPairs = calculateSiblingOverlaps(brokersAtThisLevel);
                    double overlapPercent = (double) overlappingPairs / totalPossiblePairs * 100.0;
                    
                    logger.info(String.format(
                        "    -> Overlapping Sibling Pairs: %d / %d (%.4f%%)", 
                        overlappingPairs,
                        totalPossiblePairs,
                        overlapPercent
                    ));
                }
                
                // Print details for Level 1 (Continents)
                if (currentLevel == 1) {
                    logger.info("  --- Detailed Region Info for Level 1 (Continents) ---");
                    // Get the visualizer instance
                    SimulationVisualiser visualizer = SimulationVisualiser.getInstance();
                    
                    for (BrokerWithRegion broker : brokersAtThisLevel) {
                        Region region = broker.getRegion();
                        String regionInfo = "N/A";
                        if (region != null && region.getBottomLeft() != null) {
                            regionInfo = region.toShortString(); 
                        }
                        logger.info(String.format("    - %s: Region: %s",
                                                  broker.getName(),
                                                  regionInfo));
                                                  
                        // Send this region to the map
                        visualizer.updateRegion(broker.getName(), region);
                    }
                    logger.info("  -----------------------------------------------------");
                }
                
                currentLevel++;
            }
        }
        logger.info("--- End of Topology Summary ---");
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
    
    

     /**
     * NEW REFACTORED HELPER METHOD
     * Generates a combined path string (e.g., "BrokerName [Region] -> ...")
     * for any event that implements TrackableEvent.
     * @param event The subscription or publication to process.
     * @return A formatted path string.
     */
    protected String getCombinedPathString(TrackableEvent event) {
        List<String> pathNames = event.getBrokerPath();
        List<String> pathRegions = event.getBrokerRegionPath();
        List<String> combinedPath = new ArrayList<>();

        // Zip the two lists together
        int size = Math.min(pathNames.size(), pathRegions.size());
        for (int k = 0; k < size; k++) {
            // Format: BrokerName [Region]
            combinedPath.add(pathNames.get(k) + " " + pathRegions.get(k));
        }
        
        return String.join(" -> ", combinedPath);
    }

    
    protected void collectAndPrintMetrics() {
        logger.info("\n--- Simulation Metrics ---");
        long totalSubscriptionTableEntries = 0, totalRegionUpdates = 0;
        
        long totalPublicationsSent = 0;
        long totalPropagationFilterExpansions = 0; 
        long totalMainTableExpansions = 0; 

        long totalSubscriptionProcessingEvents = 0;
        
        List<Integer> finalPublicationHops = new ArrayList<>();
        
        // List to hold data for publication_paths.csv
        List<Map<String, Object>> publicationPathData = new ArrayList<>();

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
            
            if (broker instanceof BrokerWithRegion br) {
                totalRegionUpdates += br.getNumOfRegionUpdates();
                totalPropagationFilterExpansions += br.getNumPropagationFilterExpansions();
                totalMainTableExpansions += br.getNumMainTableExpansions(); 
            }
            
            totalSubscriptionProcessingEvents += broker.getTotalSubscriptionProcessingEvents();
            allPublicationProcessingCosts.addAll(broker.getPublicationProcessingCosts());
        }
        
        for (SubscriberWithLocation subscriber : allSubscribers) {
            successfulNotifications += subscriber.getnPublications();
            allDeliveredPubHops.addAll(subscriber.getReceivedPublicationHops());
        }
        
        for (PublisherWithLocation publisher : allPublishers) {
            totalPublicationsSent += publisher.getnPublications();
            
            // Get final hop counts AND PATHS from original publications
            for (SimulationPublication pub : publisher.getSentPublications()) {
                finalPublicationHops.add(pub.getHops());

                Map<String, Object> row = new HashMap<>();
                row.put("publication_id", pub.getId());
                row.put("source_name", (pub.getSource() != null) ? pub.getSource().getName() : "N/A");
                row.put("hop_count", pub.getHops());
                row.put("publication_location", pub.toDisplayString());
                row.put("broker_path", getCombinedPathString(pub)); 
                publicationPathData.add(row);
            }
        }

        logger.info("\n--- System Overhead Metrics ---");
        logger.info("Total Subscription Table Entries Created (Storage Cost): " + totalSubscriptionTableEntries);
        logger.info("Total Region Boundary Updates (Topology CPU Cost): " + totalRegionUpdates);
        logger.info("Total Main Subscription Table Expansions (CPU Cost): " + totalMainTableExpansions);
        logger.info("Total Propagation Filter Expansions (CPU Cost): " + totalPropagationFilterExpansions);
        logger.info("Total Subscription Processing Events (CPU Cost): " + totalSubscriptionProcessingEvents);
        printStats("Final Publication Hops (Network Load)", finalPublicationHops);
        printStatsLong("Publication Processing Cost (CPU Load)", allPublicationProcessingCosts);
        
        logger.info("\n--- Service Delivery Metrics ---");
        logger.info("Total Publications Sent by all Replicas: " + totalPublicationsSent);
        logger.info("Total Successful Notifications Received by Subscribers: " + successfulNotifications);
        printStats("Delivered Publication Hops (Path Length)", allDeliveredPubHops);
        
        if (enableCsvOutput) {
            String timestamp = this.simulationTimestamp; 
            String outputDir = "output/metrics/";
            logger.info("\n--- Writing raw metrics to CSV files (Run ID: " + timestamp + ") ---");
            
            logger.warning("  ... Skipping subscription_hops.csv (data will be written by subclass in AbstractRegionPerformanceSimulation).");
                
            // Correct the variable names to match those defined at the start of this method.
            CsvMetricWriter.writeListToCsv(
                outputDir + timestamp + "_publication_hops.csv", 
                "hop_count", 
                finalPublicationHops); // FIX: Was pubHops
                
            CsvMetricWriter.writeListToCsv(
                outputDir + timestamp + "_publication_processing_cost.csv", 
                "processing_cost", 
                allPublicationProcessingCosts); // FIX: Was pubCosts
                
            CsvMetricWriter.writeListToCsv(
                outputDir + timestamp + "_delivered_publication_hops.csv", 
                "hop_count", 
                allDeliveredPubHops); // FIX: Was deliveredHops
            
            String pubPathCsvPath = outputDir + timestamp + "_publication_paths.csv";
            logger.info("  ... Writing detailed publication paths to " + pubPathCsvPath.replace(outputDir, ""));
            CsvMetricWriter.writeMapListToCsv(
                pubPathCsvPath,
                new String[]{"publication_id", "source_name", "hop_count", "publication_location", "broker_path"},
                publicationPathData
            );
        }
    }

    
    protected void printStats(String name, List<Integer> data) {
        if (data == null || data.isEmpty()) {
            logger.info(String.format("%s: N/A (no data)", name));
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

        logger.info(String.format("%s: Avg=%.2f, StdDev=%.2f, Variance=%.2f (N=%d)", 
                                  name, mean, stdDev, variance, data.size()));
    }
    
    protected void printStatsLong(String name, List<Long> data) {
        if (data == null || data.isEmpty()) {
            logger.info(String.format("%s: N/A (no data)", name));
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

        logger.info(String.format("%s: Avg=%.2f, StdDev=%.2f, Variance=%.2f (N=%d)", 
                                  name, mean, stdDev, variance, data.size()));
    }


    protected Location getRandomLocationInRegion(Region region) {
        Random rand = new Random();
        if (region == null || region.getBottomLeft() == null || region.getTopRight() == null) {
            logger.warning("Attempted to get random location in null or incomplete region.");
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
