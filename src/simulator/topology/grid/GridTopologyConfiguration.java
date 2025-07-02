package simulator.topology.grid;

import simulator.topology.TopologyConfiguration;

/**
 * Configuration for the GridTopologyGenerator.
 * It includes parameters to control the grid size, number of leaf brokers,
 * tree depth, and region overlap.
 */
public class GridTopologyConfiguration implements TopologyConfiguration {

    private final double overlapFactor;
    private final int gridDimension;
    private final int treeDepth;

    /**
     * Default constructor. Creates a 10x10 grid (100 leaves) with a depth of 4 and no overlap.
     */
    public GridTopologyConfiguration() {
        this(10, 0.0, 4);
    }

    /**
     * Constructor to create a configuration with specific parameters.
     * @param gridDimension The side length of the grid of leaf brokers (e.g., 10 for a 10x10 grid).
     * @param overlapFactor A value between 0.0 (no overlap) and 1.0 (high overlap).
     * @param treeDepth The total depth of the broker hierarchy tree. Must be >= 2.
     */
    public GridTopologyConfiguration(int gridDimension, double overlapFactor, int treeDepth) {
        if (gridDimension <= 0) {
            throw new IllegalArgumentException("Grid dimension must be positive.");
        }
        if (treeDepth < 2) {
            throw new IllegalArgumentException("Tree depth must be at least 2 (root + leaves).");
        }
        this.gridDimension = gridDimension;
        this.overlapFactor = Math.max(0.0, Math.min(1.0, overlapFactor));
        this.treeDepth = treeDepth;
    }

    public double getOverlapFactor() {
        return overlapFactor;
    }

    public int getGridDimension() {
        return gridDimension;
    }

    public int getNumberOfLeafBrokers() {
        return gridDimension * gridDimension;
    }

    public int getTreeDepth() {
        return treeDepth;
    }

    @Override
    public String toString() {
        return "GridTopologyConfiguration{" +
               "overlapFactor=" + overlapFactor +
               ", gridDimension=" + gridDimension +
               ", treeDepth=" + treeDepth +
               '}';
    }
}
