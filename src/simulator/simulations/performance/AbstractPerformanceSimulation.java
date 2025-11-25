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

import simulator.core.SimulationRunner;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.population.ProportionalSubscribersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.population.TopologyPopulator;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.visualisation.SimulationVisualiser;
import utils.CsvMetricWriter;
import utils.CustomLogger;

public abstract class AbstractPerformanceSimulation<
    C extends TopologyConfiguration, 
    F extends AbstractTopologyFactory<C, BoundedBroker>
> extends SimulationRunner<C, BoundedBroker, F> {

    private static final Logger logger = CustomLogger.getLogger(AbstractPerformanceSimulation.class.getName());

    protected final List<SubscriberWithLocation> allSubscribers = new ArrayList<>();
    protected final List<PublisherWithLocation> allPublishers = new ArrayList<>();
    protected final Random random = new Random();
    
    protected long successfulNotifications = 0;
    protected long totalSubscriptionTableEntries = 0;
    protected long totalRegionUpdates = 0;
    protected long totalPublicationsSent = 0;
    protected long totalPropagationFilterExpansions = 0;
    protected long totalMainTableExpansions = 0;
    protected long totalSubscriptionProcessingEvents = 0;

    protected abstract PublishersPlacementStrategy getPublisherPlacementStrategy();


    @Override
    protected Level getLogLevel() {
        return Level.INFO;
    }


    @Override
    protected void setupSimulation() {
        GeoNamesTopologyConfiguration geoNamesConfiguration = (GeoNamesTopologyConfiguration) this.topologyConfig;

        CsvMetricWriter.getInstance().initialize(this.simulationTimestamp);

        logger.info("\n--- Populating Topology for Performance Simulation ---");
        logger.info(String.format("Client Setup: %d Replicas, %d Subscribers/Replica", 
                                  geoNamesConfiguration.getNumberOfReplicas(), 
                                  geoNamesConfiguration.getSubscribersPerReplica()));

        if (this.rootNode == null) {
            logger.severe("Cannot populate topology: Root node is null.");
            return;
        }

        logTopologySummary(this.rootNode);

        List<BoundedBroker> leafBrokers = findLeafBrokers(this.rootNode);
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
        
        populater.populate(this.rootNode, leafBrokers, 
            geoNamesConfiguration.getTotalSubscribers(),
            geoNamesConfiguration.getNumberOfReplicas()
        );
        
        collectClients(leafBrokers);
    }


    // Plane Sweep Algorithm for Overlap Calculation
    private static class SweepEvent implements Comparable<SweepEvent> {
        enum EventType { START, END }
        final double x;
        final EventType type;
        final BoundedBroker broker;

        SweepEvent(double x, EventType type, BoundedBroker broker) {
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

    private long calculateSiblingOverlaps(List<BoundedBroker> brokers) {
        List<SweepEvent> events = new ArrayList<>(brokers.size() * 2);
        for (BoundedBroker broker : brokers) {
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
        Map<BoundedBroker, Integer> activeSegments = new HashMap<>();
        Set<String> countedPairs = new HashSet<>(); 

        for (SweepEvent event : events) {
            BoundedBroker eventBroker = event.broker;
            Region r1 = eventBroker.getRegion();
            if (event.type == SweepEvent.EventType.START) {
                for (BoundedBroker activeBroker : activeSegments.keySet()) {
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

    protected void logTopologySummary(BoundedBroker root) {
        if (root == null) return;
        logger.info("\n--- Broker Topology Structure Summary ---");
        Queue<BoundedBroker> queue = new LinkedList<>();
        queue.add(root);
        int currentLevel = 0;

        while (!queue.isEmpty()) {
            int levelSize = queue.size(); 
            long totalBrokerChildrenAtLevel = 0; 
            List<BoundedBroker> brokersAtThisLevel = new ArrayList<>(levelSize);

            for (int i = 0; i < levelSize; i++) {
                BoundedBroker broker = queue.poll();
                if (broker == null) continue;
                brokersAtThisLevel.add(broker); 
                if (broker.getChildren() != null) {
                    for (TreeNode child : broker.getChildren()) {
                        if (child instanceof BoundedBroker childBroker) { 
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
                    for (BoundedBroker broker : brokersAtThisLevel) {
                        visualizer.updateRegion(broker.getName(), broker.getRegion());
                    }
                }
                currentLevel++;
            }
        }
        logger.info("--- End of Topology Summary ---");
    }
    
    private void collectClients(List<BoundedBroker> leafBrokers) {
        allSubscribers.clear();
        allPublishers.clear();
        for (BoundedBroker leaf : leafBrokers) {
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
    
    protected List<BoundedBroker> findLeafBrokers(BoundedBroker root) {
        List<BoundedBroker> leaves = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);
        while(!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof BoundedBroker) {
                boolean hasBrokerChild = false;
                for (TreeNode child : current.getChildren()) {
                    if (child instanceof BoundedBroker) hasBrokerChild = true;
                }
                if (!hasBrokerChild) leaves.add((BoundedBroker) current);
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
            totalSubscriptionTableEntries += broker.getSubscriptionCount();
            totalSubscriptionProcessingEvents += broker.getTotalSubscriptionProcessingEvents();
            
            if (broker instanceof BoundedBroker br) {
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

        CsvMetricWriter.getInstance().close();
        logger.info("Metrics writer closed.");
        
        SimulationVisualiser visualizer = SimulationVisualiser.getInstance();
        visualizer.saveMapImage(simulationTimestamp);
        visualizer.close();
    }
}