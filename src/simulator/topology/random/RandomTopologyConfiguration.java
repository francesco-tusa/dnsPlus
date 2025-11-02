package simulator.topology.random;

import simulator.topology.TopologyConfiguration;

/**
 * Base configuration class for random topology generation.
 * Holds parameters common to various tree-based random topologies.
 * Now includes world dimensions.
 */
public class RandomTopologyConfiguration implements TopologyConfiguration {
    private final int treeDepth;
    private final int maxBranchingFactor;
    private final int subscribersPerLeafNode;
    private final int publishersPerLeafNode;
    
    // New parameters for configurable world size
    private final double worldWidth;
    private final double worldHeight;

    public RandomTopologyConfiguration(int treeDepth, int maxBranchingFactor, 
                                       int subscribersPerLeafNode, int publishersPerLeafNode) {
        // Call new constructor with default world size for backward compatibility
        this(treeDepth, maxBranchingFactor, subscribersPerLeafNode, publishersPerLeafNode, 1000.0, 1000.0);
    }

    /**
     * Main constructor including world dimensions.
     */
    public RandomTopologyConfiguration(int treeDepth, int maxBranchingFactor, 
                                       int subscribersPerLeafNode, int publishersPerLeafNode,
                                       double worldWidth, double worldHeight) {
        // Basic validation
        if (treeDepth < 1) throw new IllegalArgumentException("Tree depth must be at least 1.");
        if (maxBranchingFactor < 1) throw new IllegalArgumentException("Maximum branching factor must be at least 1.");
        if (subscribersPerLeafNode < 0) throw new IllegalArgumentException("Subscribers per leaf node cannot be negative.");
        if (publishersPerLeafNode < 0) throw new IllegalArgumentException("Publishers per leaf node cannot be negative.");
        if (worldWidth <= 0) throw new IllegalArgumentException("World width must be positive.");
        if (worldHeight <= 0) throw new IllegalArgumentException("World height must be positive.");

        this.treeDepth = treeDepth;
        this.maxBranchingFactor = maxBranchingFactor;
        this.subscribersPerLeafNode = subscribersPerLeafNode;
        this.publishersPerLeafNode = publishersPerLeafNode;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }


    // --- Getters ---

    public int getTreeDepth() {
        return treeDepth;
    }

    /**
     * Gets the maximum number of children a non-leaf node can have.
     * The actual number will be chosen randomly between 1 and this value (inclusive).
     * @return the maximum branching factor.
     */
    public int getMaxBranchingFactor() {
        return maxBranchingFactor;
    }

    public int getSubscribersPerLeafNode() {
        return subscribersPerLeafNode;
    }

    public int getPublishersPerLeafNode() {
        return publishersPerLeafNode;
    }
    
    public double getWorldWidth() {
        return worldWidth;
    }

    public double getWorldHeight() {
        return worldHeight;
    }
}