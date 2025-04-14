package simulator.topology.random;

import simulator.topology.TopologyConfiguration;

/**
 * Base configuration class for random topology generation.
 * Holds parameters common to various tree-based random topologies.
 */
public class RandomTopologyConfiguration implements TopologyConfiguration {
    private final int treeDepth;
    private final int maxBranchingFactor;
    private final int subscribersPerLeafNode;
    private final int publishersPerLeafNode;

    public RandomTopologyConfiguration(int treeDepth, int maxBranchingFactor, int subscribersPerLeafNode, int publishersPerLeafNode) {
        // Basic validation
        if (treeDepth < 1) throw new IllegalArgumentException("Tree depth must be at least 1.");
        // Allow maxBranchingFactor of 1 (results in a chain)
        if (maxBranchingFactor < 1) throw new IllegalArgumentException("Maximum branching factor must be at least 1.");
        if (subscribersPerLeafNode < 0) throw new IllegalArgumentException("Subscribers per leaf node cannot be negative.");
        if (publishersPerLeafNode < 0) throw new IllegalArgumentException("Publishers per leaf node cannot be negative.");

        this.treeDepth = treeDepth;
        this.maxBranchingFactor = maxBranchingFactor;
        this.subscribersPerLeafNode = subscribersPerLeafNode;
        this.publishersPerLeafNode = publishersPerLeafNode;
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
}
