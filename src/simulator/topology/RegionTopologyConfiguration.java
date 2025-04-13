package simulator.topology;

/**
 * Configuration specific to generating topologies with regions.
 * Extends the base configuration with region-related parameters.
 * The number of leaf brokers generated depends on the tree structure,
 * not a fixed number per region.
 */
public class RegionTopologyConfiguration extends TopologyConfiguration {
    private final int numRegions;

    public RegionTopologyConfiguration(int treeDepth, int maxBranchingFactor, int numRegions, int subscribersPerLeafBroker, int publishersPerLeafBroker) {
        // Pass maxBranchingFactor to the super constructor
        super(treeDepth, maxBranchingFactor, subscribersPerLeafBroker, publishersPerLeafBroker);

        // Region-specific validation
        if (numRegions < 1) throw new IllegalArgumentException("Number of regions must be at least 1.");
        // Removed validation for leafBrokersPerRegion

        this.numRegions = numRegions;
        // Removed: this.leafBrokersPerRegion = leafBrokersPerRegion;
    }

    // --- Getters ---

    public int getNumRegions() {
        return numRegions;
    }

    // Removed: public int getLeafBrokersPerRegion()
}
