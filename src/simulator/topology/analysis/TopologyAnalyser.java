package simulator.topology.analysis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.logging.Logger;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.regions.BoundedBroker;
import simulator.regions.LeafBroker;

public class TopologyAnalyser {

    // --- Stats Container ---
    public static class TopologyStats {
        public final long totalBrokers;
        public final long totalLeafBrokers;
        
        public TopologyStats(long totalBrokers, long totalLeafBrokers) {
            this.totalBrokers = totalBrokers;
            this.totalLeafBrokers = totalLeafBrokers;
        }
    }

    // --- Graph Traversal Utilities ---

    public static <T extends TreeNode> T findFirstNode(TreeNode root, Class<T> type, Predicate<T> condition) {
        if (root == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        // Optimization: Detect if we are searching for BoundedBroker (or a subclass)
        boolean searchingForBroker = BoundedBroker.class.isAssignableFrom(type);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (type.isInstance(current)) {
                T casted = type.cast(current);
                if (condition.test(casted)) return casted;
            }
            
            if (current.getChildren() != null) {
                if (searchingForBroker) {
                    // SAFE PATH: Only queue Brokers, skip 50M subscribers
                    for (TreeNode child : current.getChildren()) {
                        if (child instanceof BoundedBroker) {
                            queue.add(child);
                        }
                    }
                } else {
                    // GENERIC PATH: Must queue everything
                    queue.addAll(current.getChildren());
                }
            }
        }
        return null;
    }

    public static <T extends TreeNode> T findNodeByName(TreeNode root, String name, Class<T> type) {
        return findFirstNode(root, type, node -> name.equals(node.getName()));
    }

    public static <T extends TreeNode> T findNodeByNameContains(TreeNode root, String partialName, Class<T> type) {
        return findFirstNode(root, type, node -> node.getName().contains(partialName));
    }

    public static BoundedBroker findLeafBrokerAtLocation(BoundedBroker root, Location location) {
        return findFirstNode(root, BoundedBroker.class, broker -> 
            isLeafBroker(broker) && 
            broker.getRegion() != null && 
            broker.getRegion().contains(location)
        );
    }

    public static BoundedBroker findBrokerByName(BoundedBroker root, String name) {
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
            if (current.getNodeLevel() == targetLevel) result.add(current);
            else if (current.getNodeLevel() < targetLevel) addBrokerChildrenToQueue(current, queue);
        }
        return result;
    }

    public static List<BoundedBroker> findLeafBrokers(BoundedBroker root) {
        List<BoundedBroker> leaves = new ArrayList<>();
        if (root == null) return leaves;

        Queue<BoundedBroker> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            BoundedBroker current = queue.poll();
            
            if (isLeafBroker(current)) {
                leaves.add(current);
            } else {
                // Only traverse down if it's NOT a leaf
                if (current.getChildren() != null) {
                    for (TreeNode child : current.getChildren()) {
                        if (child instanceof BoundedBroker broker) {
                            queue.add(broker); 
                        }
                    }
                }
            }
        }
        return leaves;
    }

    public static boolean isLeafBroker(BoundedBroker broker) {
        return broker instanceof LeafBroker;
    }

    public static BoundedBroker findFirstLeafBroker(TreeNode root) {
        return findFirstNode(root, BoundedBroker.class, TopologyAnalyser::isLeafBroker);
    }

    public static TopologyStats logStructure(BoundedBroker root, Logger logger) {
        if (root == null) return new TopologyStats(0, 0);
        
        logger.info("");
        logger.info("--- Broker Topology Structure Summary (Runtime) ---");
        
        Queue<BoundedBroker> queue = new LinkedList<>();
        queue.add(root);
        int currentLevel = 0;
        
        long totalBrokers = 0;
        long totalLeafBrokers = 0;

        while (!queue.isEmpty()) {
            int levelSize = queue.size(); 
            long totalBrokerChildrenAtLevel = 0; 
            int levelLeafCount = 0; 
            
            totalBrokers += levelSize;

            for (int i = 0; i < levelSize; i++) {
                BoundedBroker broker = queue.poll();
                if (broker == null) continue;
                
                if (isLeafBroker(broker)) {
                    levelLeafCount++;
                }

                if (broker.getChildren() != null) {
                    for (TreeNode child : broker.getChildren()) {
                        if (child instanceof BoundedBroker childBroker) { 
                            totalBrokerChildrenAtLevel++;
                            queue.add(childBroker); 
                        }
                    }
                }
            }
            
            totalLeafBrokers += levelLeafCount;

            if (levelSize > 0) {
                double avgFanOut = (totalBrokerChildrenAtLevel > 0) ? (double) totalBrokerChildrenAtLevel / levelSize : 0.0;
                
                logger.info(String.format("  Level %d: %d brokers (%d leaves), Avg. Fan-Out: %.2f", 
                    currentLevel, levelSize, levelLeafCount, avgFanOut));
                
                currentLevel++;
            }
        }
        
        logger.info("-----------------------------------------------");
        logger.info(String.format("  Total Brokers: %d", totalBrokers));
        logger.info(String.format("  Total Leaf Brokers: %d", totalLeafBrokers));
        logger.info("--- End of Topology Summary ---");
        
        return new TopologyStats(totalBrokers, totalLeafBrokers);
    }
}