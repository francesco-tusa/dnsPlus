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
import simulator.population.ProportionalSubscribersPlacement;
import simulator.population.PublishersPlacementStrategy;
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
    protected final boolean enableCsvOutput;
    
    // --- Simplified Metrics (Counters Only) ---
    protected long successfulNotifications = 0;
    protected long totalSubscriptionTableEntries = 0;
    protected long totalRegionUpdates = 0;
    protected long totalPublicationsSent = 0;
    protected long totalPropagationFilterExpansions = 0;
    protected long totalMainTableExpansions = 0;
    protected long totalSubscriptionProcessingEvents = 0;

    protected abstract PublishersPlacementStrategy getPublisherPlacementStrategy();

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
        if (enableCsvOutput) {
            CsvMetricWriter.getInstance().initialize(this.simulationTimestamp);
        }

        logger.info("\n--- Populating Topology for Performance Simulation ---");
        logger.info(String.format("Client Setup: %d Replicas, %d Subscribers/Replica (Total Subscribers: %d)", 
                                  numberOfReplicas, subscribersPerReplica, totalSubscribers));

        if (this.topologyConfig instanceof RegionRandomTopologyConfiguration config) {
            logger.info(String.format("Topology Setup (Random): Depth=%d, MaxBranch=%d, NumRegions=%d",
                config.getTreeDepth(), config.getMaxBranchingFactor(), config.getNumRegions()));
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
        
        PublishersPlacementStrategy strategy = getPublisherPlacementStrategy();
        logger.info("Using Publisher Placement Strategy: " + strategy.getClass().getSimpleName());
        
        TopologyPopulator populater = new TopologyPopulator(
            new ProportionalSubscribersPlacement(), 
            strategy
        );
        
        populater.populate(this.rootNode, leafBrokers, getTotalSubscribers(), getNumberOfReplicas());
        collectClients(leafBrokers);
    }
    
    private void logBrokerHierarchy(TreeNode node, String indent) {
        if (node instanceof BrokerWithRegion broker) {
            Region region = broker.getRegion();
            String regionInfo = (region != null) ? region.toShortString() : "N/A"; // Safe string
            logger.fine(String.format("%s%s [%s]", indent, broker.getName(), regionInfo));
            for (TreeNode child : broker.getChildren()) {
                logBrokerHierarchy(child, indent + "  ");
            }
        }
    }

    // Plane Sweep Algorithm for Overlap Calculation
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
            int xCompare = Double.compare(this.x, other.x);
            if (xCompare != 0) return xCompare;
            return this.type.compareTo(other.type);
        }
    }

    private long calculateSiblingOverlaps(List<BrokerWithRegion> brokers) {
        List<SweepEvent> events = new ArrayList<>(brokers.size() * 2);
        for (BrokerWithRegion broker : brokers) {
            Region region = broker.getRegion();
            if (region == null || region.getBottomLeft() == null || region.getTopRight() == null) continue;
            double minLon = region.getBottomLeft().getX();
            double maxLon = region.getTopRight().getX();
            if (minLon <= maxLon) {
                events.add(new SweepEvent(minLon, SweepEvent.EventType.START, broker));
                events.add(new SweepEvent(maxLon, SweepEvent.EventType.END, broker));
            } else {
                events.add(new SweepEvent(minLon, SweepEvent.EventType.START, broker));
                events.add(new SweepEvent(180.0, SweepEvent.EventType.END, broker));
                events.add(new SweepEvent(-180.0, SweepEvent.EventType.START, broker));
                events.add(new SweepEvent(maxLon, SweepEvent.EventType.END, broker));
            }
        }
        Collections.sort(events);

        long overlapCount = 0;
        Map<BrokerWithRegion, Integer> activeSegments = new HashMap<>();
        Set<String> countedPairs = new HashSet<>(); 

        for (SweepEvent event : events) {
            BrokerWithRegion eventBroker = event.broker;
            Region r1 = eventBroker.getRegion();
            if (event.type == SweepEvent.EventType.START) {
                for (BrokerWithRegion activeBroker : activeSegments.keySet()) {
                    if (activeBroker == eventBroker) continue;
                    String pairKey = (eventBroker.getName().compareTo(activeBroker.getName()) < 0)
                                     ? eventBroker.getName() + "::" + activeBroker.getName()
                                     : activeBroker.getName() + "::" + eventBroker.getName();
                    if (countedPairs.contains(pairKey)) continue;
                    if (r1.intersects(activeBroker.getRegion())) {
                        overlapCount++;
                        countedPairs.add(pairKey);
                    }
                }
                activeSegments.put(eventBroker, activeSegments.getOrDefault(eventBroker, 0) + 1);
            } else { 
                int count = activeSegments.getOrDefault(eventBroker, 0);
                if (count <= 1) activeSegments.remove(eventBroker);
                else activeSegments.put(eventBroker, count - 1);
            }
        }
        return overlapCount;
    }

    protected void logTopologySummary(BrokerWithRegion root) {
        if (root == null) return;
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
                double avgFanOut = (totalBrokerChildrenAtLevel > 0) ? (double) totalBrokerChildrenAtLevel / levelSize : 0.0;
                logger.info(String.format("  Level %d: %d brokers, Avg. Fan-Out: %.2f", currentLevel, levelSize, avgFanOut));

                long totalPossiblePairs = (long) levelSize * (levelSize - 1) / 2;
                if (totalPossiblePairs > 0) {
                    long overlappingPairs = calculateSiblingOverlaps(brokersAtThisLevel);
                    double overlapPercent = (double) overlappingPairs / totalPossiblePairs * 100.0;
                    logger.info(String.format("    -> Overlapping Sibling Pairs: %d / %d (%.4f%%)", overlappingPairs, totalPossiblePairs, overlapPercent));
                }
                
                if (currentLevel == 1) {
                    SimulationVisualiser visualizer = SimulationVisualiser.getInstance();
                    for (BrokerWithRegion broker : brokersAtThisLevel) {
                        visualizer.updateRegion(broker.getName(), broker.getRegion());
                    }
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
                    if (child instanceof BrokerWithRegion) hasBrokerChild = true;
                }
                if (!hasBrokerChild) leaves.add((BrokerWithRegion) current);
            }
            if (current.getChildren() != null) queue.addAll(current.getChildren());
        }
        return leaves;
    }
    
    /**
     * Collects high-level metrics from brokers and clients.
     * detailed metrics are streamed to CSV.
     */
    protected void collectAndPrintMetrics() {
        logger.info("\n--- Simulation Metrics Summary ---");
        
        // Reset counters
        totalSubscriptionTableEntries = 0;
        totalRegionUpdates = 0;
        totalPublicationsSent = 0;
        totalPropagationFilterExpansions = 0;
        totalMainTableExpansions = 0;
        totalSubscriptionProcessingEvents = 0;
        successfulNotifications = 0;

        // Collect Broker Metrics
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
            totalSubscriptionProcessingEvents += broker.getTotalSubscriptionProcessingEvents();
            
            if (broker instanceof BrokerWithRegion br) {
                totalRegionUpdates += br.getNumOfRegionUpdates();
                totalPropagationFilterExpansions += br.getNumPropagationFilterExpansions();
                totalMainTableExpansions += br.getNumMainTableExpansions(); 
            }
        }
        
        // Collect Client Metrics
        for (SubscriberWithLocation subscriber : allSubscribers) {
            successfulNotifications += subscriber.getnPublications();
        }
        
        for (PublisherWithLocation publisher : allPublishers) {
            totalPublicationsSent += publisher.getnPublications();
        }

        // Log Summary
        logger.info("\n--- System Overhead Metrics ---");
        logger.info("Total Subscription Table Entries: " + totalSubscriptionTableEntries);
        logger.info("Total Region Updates: " + totalRegionUpdates);
        logger.info("Total Subscription Process Events: " + totalSubscriptionProcessingEvents);
        logger.info("Total Expansion Events (Main/Filter): " + totalMainTableExpansions + " / " + totalPropagationFilterExpansions);

        logger.info("\n--- Service Delivery Metrics ---");
        logger.info("Total Publications Sent: " + totalPublicationsSent);
        logger.info("Total Notifications Received: " + successfulNotifications);
    }


    @Override
    protected void cleanup() {
        super.cleanup();
        if (enableCsvOutput) {
            CsvMetricWriter.getInstance().close();
            logger.info("Metrics writer closed.");
        }
        SimulationVisualiser visualizer = SimulationVisualiser.getInstance();
        visualizer.saveMapImage(simulationTimestamp);
        visualizer.close();
    }
    
    protected Location getRandomLocationInRegion(Region region) {
        Random rand = new Random();
        if (region == null || region.getBottomLeft() == null) return new Location(0, 0, 0);
        double minX = region.getBottomLeft().getX();
        double maxX = region.getTopRight().getX();
        double minY = region.getBottomLeft().getY();
        double maxY = region.getTopRight().getY();
        double x = minX + (maxX - minX) * rand.nextDouble();
        double y = minY + (maxY - minY) * rand.nextDouble();
        return new Location(x, y, 0);
    }
}