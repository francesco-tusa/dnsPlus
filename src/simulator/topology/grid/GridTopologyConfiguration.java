package simulator.topology.grid;

import java.util.logging.Logger;
import simulator.config.SimConfiguration;
import simulator.topology.TopologyConfiguration;

/**
 * Configuration for the GridTopologyGenerator.
 * Loads parameters from the centralized SimConfiguration by default.
 */
public class GridTopologyConfiguration implements TopologyConfiguration {

    private final int gridDimension;
    private final double overlapFactor;
    private final int treeDepth;
    private final int subscribersPerLeaf;
    private final int publishersPerLeaf;

    /**
     * Default constructor: Loads parameters from SimConfiguration.
     */
    public GridTopologyConfiguration() {
        SimConfiguration config = SimConfiguration.get();
        
        this.gridDimension = config.topology.gridDimension;
        this.overlapFactor = config.topology.overlapFactor; 
        this.treeDepth = config.topology.treeDepth;
        
        this.subscribersPerLeaf = config.workload.subscribersPerLeafNode;
        this.publishersPerLeaf = config.workload.publishersPerLeafNode;
    }

    /**
     * Manual Constructor for specific tests (e.g. Functional Tests).
     */
    public GridTopologyConfiguration(int gridDimension, double overlapFactor, int treeDepth, int subscribersPerLeaf, int publishersPerLeaf) {
        this.gridDimension = gridDimension;
        this.overlapFactor = overlapFactor;
        this.treeDepth = treeDepth;
        this.subscribersPerLeaf = subscribersPerLeaf;
        this.publishersPerLeaf = publishersPerLeaf;
        validate();
    }
    
    // Legacy constructor for tests
    public GridTopologyConfiguration(int gridDimension, double overlapFactor, int treeDepth) {
        this(gridDimension, overlapFactor, treeDepth, 1, 1);
    }

    private void validate() {
        if (gridDimension <= 0) throw new IllegalArgumentException("Grid dimension must be positive.");
        if (treeDepth < 2) throw new IllegalArgumentException("Tree depth must be at least 2.");
    }

    public double getOverlapFactor() { return overlapFactor; }
    public int getGridDimension() { return gridDimension; }
    public int getNumberOfLeafBrokers() { return gridDimension * gridDimension; }
    public int getTreeDepth() { return treeDepth; }
    public int getSubscribersPerLeaf() { return subscribersPerLeaf; }
    public int getPublishersPerLeaf() { return publishersPerLeaf; }

    @Override
    public void logDetails(Logger logger) {
        logger.info(String.format(
            "Topology Setup (Grid): Dimension=%dx, Overlap=%.2f, Depth=%d, Subs/Leaf=%d, Pubs/Leaf=%d",
            gridDimension, overlapFactor, treeDepth, subscribersPerLeaf, publishersPerLeaf
        ));
    }

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
