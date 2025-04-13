package simulator.topology;

import simulator.TreeNode;

/**
 * Interface defining the contract for topology generators/factories.
 * Implementations create a simulation topology based on a configuration.
 */
public interface TopologyFactory {

    /**
     * Generates the simulation topology based on the provided configuration.
     *
     * @param config The configuration object containing parameters for generation.
     *               The specific implementation may expect a subclass of TopologyConfiguration.
     * @return The root TreeNode of the generated topology.
     * @throws IllegalArgumentException if the configuration is invalid or not suitable
     *                                  for the specific generator implementation.
     */
    TreeNode generateTopology(TopologyConfiguration config);

}