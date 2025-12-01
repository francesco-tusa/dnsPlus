package simulator.topology.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import simulator.visualisation.SimulationVisualiser;

/**
 * Utility class for analyzing topology structure, geometric properties, and graph traversal.
 */
public class TopologyAnalyzer {

    // --- Graph Traversal Utilities ---

    /**
     * Generic Breadth-First Search to find the first node matching a condition.
     * This replaces specific traversal loops with a reusable pattern.
     *
     * @param root The starting node.
     * @param type The class type of the node to find.
     * @param condition The condition to match.
     * @return The found node, or null.
     */
    public static <T extends TreeNode> T findFirstNode(TreeNode root, Class<T> type, Predicate<T> condition) {
        if (root == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            
            // Check match
            if (type.isInstance(current)) {
                T casted = type.cast(current);
                if (condition.test(casted)) {
                    return casted;
                }
            }
            
            // Continue traversal
            if (current.getChildren() != null) {
                queue.addAll(current.getChildren());
            }
        }
        return null;
    }

    /**
     * Uses the generic findFirstNode method.
     * Finds a node in the tree by its name and expected type.
     */
    public static <T extends TreeNode> T findNodeByName(TreeNode root, String name, Class<T> type) {
        return findFirstNode(root, type, node -> name.equals(node.getName()));
    }

    /**
     * Optimized search that looks ONLY for BoundedBrokers by name.
     */
    public static BoundedBroker findBrokerByName(BoundedBroker root, String name) {
        // We keep the optimized broker-only traversal here for performance if needed, 
        // or it could also be refactored to use findFirstNode(root, BoundedBroker.class, ...)
        // For now, retaining specific implementation to restrict search scope to Brokers only (skipping clients).
        if (root == null || name == null) return null;
        if (root.getName().equals(name)) return root;

        Queue<BoundedBroker> queue = new LinkedList<>();
        addBrokerChildrenToQueue(root, queue);

        while (!queue.isEmpty()) {
            BoundedBroker current = queue.poll();
            if (current.getName().equals(name)) return current;
            addBrokerChildrenToQueue(current, queue);
        }
        return null;
    }

    /**
     * Finds the first Leaf Broker whose region contains the specified location.
     * This generalizes the geometric fallback logic previously in AwsRegions.
     */
    public static BoundedBroker findLeafBrokerAtLocation(BoundedBroker root, Location location) {
        return findFirstNode(root, BoundedBroker.class, broker -> 
            isLeafBroker(broker) && 
            broker.getRegion() != null && 
            broker.getRegion().contains(location)
        );
    }

    /**
     * Helper to determine if a broker is a leaf (has no broker children).
     */
    public static boolean isLeafBroker(BoundedBroker broker) {
        if (broker.getChildren() == null) return true;
        for (TreeNode child : broker.getChildren()) {
            if (child instanceof BoundedBroker) return false;
        }
        return true;
    }

    private static void addBrokerChildrenToQueue(BoundedBroker parent, Queue<BoundedBroker> queue) {
        if (parent.getChildren() != null) {
            for (TreeNode child : parent.getChildren()) {
                if (child instanceof BoundedBroker broker) queue.add(broker);
            }
        }
    }

    public static List<BoundedBroker> findBrokersAtLevel(BoundedBroker root, int targetLevel) {
        List<BoundedBroker> result = new ArrayList<>();
        if (root == null || root.getNodeLevel() > targetLevel) return result;

        Queue<BoundedBroker> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            BoundedBroker current = queue.poll();

            if (current.getNodeLevel() == targetLevel) {
                result.add(current);
            } else if (current.getNodeLevel() < targetLevel) {
                addBrokerChildrenToQueue(current, queue);
            }
        }
        return result;
    }

    public static List<BoundedBroker> findLeafBrokers(BoundedBroker root) {
        List<BoundedBroker> leaves = new ArrayList<>();
        // Could now use findFirstNode logic, but we need ALL leaves, not just the first.
        // Keeping manual traversal for collection.
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);
        
        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof BoundedBroker broker) {
                if (isLeafBroker(broker)) {
                    leaves.add(broker);
                }
            }
            if (current.getChildren() != null) queue.addAll(current.getChildren());
        }
        return leaves;
    }

    public static void logStructure(BoundedBroker root, Logger logger) {
        if (root == null) return;
        
        logger.info("");
        logger.info("--- Broker Topology Structure Summary ---");
        
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

    // --- Plane Sweep Algorithm ---
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

    public static long calculateSiblingOverlaps(List<BoundedBroker> brokers) {
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
}