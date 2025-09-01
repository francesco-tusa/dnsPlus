package simulator.topology.grid;

import simulator.topology.TopologyConfiguration;

/**
 * Configuration for the GridTopologyGenerator.
 * It includes parameters to control the grid size, number of leaf brokers,
 * tree depth, and the number of clients per leaf.
 */
public class GridTopologyConfiguration implements TopologyConfiguration {

    private final int gridDimension;
    private final double overlapFactor;
    private final int treeDepth;
    private final int subscribersPerLeaf;
    private final int publishersPerLeaf;

    /**
     * Default constructor for backward compatibility.
     */
    public GridTopologyConfiguration() {
        this(10, 0.0, 4, 1, 1);
    }

    /**
     * Constructor for validation tests.
     */
    public GridTopologyConfiguration(int gridDimension, double overlapFactor, int treeDepth) {
        this(gridDimension, overlapFactor, treeDepth, 1, 1);
    }
    
    /**
     * Full constructor to create a configuration with specific parameters.
     * @param gridDimension The side length of the grid of leaf brokers.
     * @param overlapFactor A value between 0.0 (no overlap) and 1.0 (high overlap).
     * @param treeDepth The total depth of the broker hierarchy tree. Must be >= 2.
     * @param subscribersPerLeaf The number of subscribers to attach to each leaf broker.
     * @param publishersPerLeaf The number of publishers to attach to each leaf broker.
     */
    public GridTopologyConfiguration(int gridDimension, double overlapFactor, int treeDepth, int subscribersPerLeaf, int publishersPerLeaf) {
        if (gridDimension <= 0) throw new IllegalArgumentException("Grid dimension must be positive.");
        if (treeDepth < 2) throw new IllegalArgumentException("Tree depth must be at least 2.");
        this.gridDimension = gridDimension;
        this.overlapFactor = Math.max(0.0, Math.min(1.0, overlapFactor));
        this.treeDepth = treeDepth;
        this.subscribersPerLeaf = subscribersPerLeaf;
        this.publishersPerLeaf = publishersPerLeaf;
    }

    public double getOverlapFactor() { return overlapFactor; }
    public int getGridDimension() { return gridDimension; }
    public int getNumberOfLeafBrokers() { return gridDimension * gridDimension; }
    public int getTreeDepth() { return treeDepth; }
    public int getSubscribersPerLeaf() { return subscribersPerLeaf; }
    public int getPublishersPerLeaf() { return publishersPerLeaf; }

    @Override
    public String toString() {
        return "GridTopologyConfiguration{" +
               "gridDimension=" + gridDimension +
               ", overlapFactor=" + overlapFactor +
               ", treeDepth=" + treeDepth +
               ", subscribersPerLeaf=" + subscribersPerLeaf +
               ", publishersPerLeaf=" + publishersPerLeaf +
               '}';
    }
}
