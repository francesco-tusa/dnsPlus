package simulator.topology.fixed;

import java.util.logging.Logger;

import simulator.topology.TopologyConfiguration;

/**
 * Configuration for the FixedTopologyGenerator.
 * This topology is hardcoded, so this class mainly serves the factory pattern.
 * We can optionally add flags here later if needed (e.g., which test scenario to run).
 */
public class FixedTestTopologyConfiguration implements TopologyConfiguration {

    // No specific parameters needed for this hardcoded topology yet.

    public FixedTestTopologyConfiguration() {
        // treeDepth, maxBranchingFactor, subsPerLeaf, pubsPerLeaf are irrelevant here
    }

    @Override
    public void logDetails(Logger logger) {
        logger.info(String.format(
            "Fixed topology setup, no parameters to display"
        ));        
    }

}

