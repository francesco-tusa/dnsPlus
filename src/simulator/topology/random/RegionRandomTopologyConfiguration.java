package simulator.topology.random;

/**
 * Configuration specific to generating random topologies with regions.
 * Extends the base configuration with region-related parameters.
 * The number of leaf brokers generated depends on the tree structure,
 * not a fixed number per region.
 */
public class RegionRandomTopologyConfiguration extends RandomTopologyConfiguration {
    private final int numRegions;

    public RegionRandomTopologyConfiguration(int treeDepth, int maxBranchingFactor, int numRegions, 
                                             int subscribersPerLeafBroker, int publishersPerLeafBroker) {
        // Call parent constructor with default world size
        super(treeDepth, maxBranchingFactor, subscribersPerLeafBroker, publishersPerLeafBroker);
        if (numRegions < 1) throw new IllegalArgumentException("Number of regions must be at least 1.");
        this.numRegions = numRegions;
    }

    /**
     * New constructor to pass world dimensions up to the parent class.
     */
    public RegionRandomTopologyConfiguration(int treeDepth, int maxBranchingFactor, int numRegions, 
                                             int subscribersPerLeafBroker, int publishersPerLeafBroker,
                                             double worldWidth, double worldHeight) {
        // Call parent constructor with specified world size
        super(treeDepth, maxBranchingFactor, subscribersPerLeafBroker, publishersPerLeafBroker, worldWidth, worldHeight);
        if (numRegions < 1) throw new IllegalArgumentException("Number of regions must be at least 1.");
        this.numRegions = numRegions;
    }

    // --- Getters ---

    public int getNumRegions() {
        return numRegions;
    }
}