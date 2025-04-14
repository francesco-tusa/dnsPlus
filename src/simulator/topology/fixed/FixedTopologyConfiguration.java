package simulator.topology.fixed;

import simulator.topology.TopologyConfiguration;

/**
 * Configuration for the FixedTopologyGenerator.
 * This topology is hardcoded, so this class mainly serves the factory pattern.
 * We can optionally add flags here later if needed (e.g., which test scenario to run).
 */
public class FixedTopologyConfiguration implements TopologyConfiguration {

    // No specific parameters needed for this hardcoded topology yet.

    public FixedTopologyConfiguration() {
        // treeDepth, maxBranchingFactor, subsPerLeaf, pubsPerLeaf are irrelevant here
    }
}

