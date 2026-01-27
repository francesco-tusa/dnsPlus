package simulator.topology.random;

import simulator.config.SimConfiguration;
import simulator.topology.TopologyConfiguration;

/**
 * Base configuration class for random topology generation.
 * Holds parameters common to various tree-based random topologies.
 * Now includes world dimensions.
 */
public abstract class RandomTopologyConfiguration implements TopologyConfiguration {
    private final int treeDepth;
    private final int maxBranchingFactor;
    private final int subscribersPerLeafNode;
    private final int publishersPerLeafNode;

    // New parameters for configurable world size
    private final double worldWidth;
    private final double worldHeight;

    private final boolean enableVerboseLogs;

    public RandomTopologyConfiguration() {
        // Load from Central Config
        SimConfiguration config = SimConfiguration.get();

        this.maxBranchingFactor = config.topology.randomMaxBranching;
        this.treeDepth = config.topology.treeDepth;
        this.worldWidth = config.topology.randomWorldWidth;
        this.worldHeight = config.topology.randomWorldHeight;
        this.subscribersPerLeafNode = config.workload.subscribersPerLeafNode;
        this.publishersPerLeafNode = config.workload.publishersPerLeafNode;
        this.enableVerboseLogs = config.paths.enableVerboseLogs;
    }

    public int getTreeDepth() {
        return treeDepth;
    }

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

    public boolean isEnableVerboseLogs() {
        return enableVerboseLogs;
    }
}