package simulator.topology.random;

import java.util.logging.Logger;

import simulator.config.SimConfiguration;

/**
 * Configuration specific to generating random topologies with regions.
 * Extends the base configuration with region-related parameters.
 * The number of leaf brokers generated depends on the tree structure,
 * not a fixed number per region.
 */
public class RegionRandomTopologyConfiguration extends RandomTopologyConfiguration {
    private final int numRegions;

    public RegionRandomTopologyConfiguration() {
        numRegions = SimConfiguration.get().topology.randomNumRegions;                   
    }

    // --- Getters ---
    public int getNumRegions() {
        return numRegions;
    }

    @Override
    public void logDetails(Logger logger) {
        logger.info(String.format(
            "Topology Setup (Random): Depth=%d, MaxBranch=%d, NumRegions=%d, WorldSize=[%.1f x %.1f]",
            getTreeDepth(), getMaxBranchingFactor(), numRegions, getWorldWidth(), getWorldHeight()
        ));
    }
}