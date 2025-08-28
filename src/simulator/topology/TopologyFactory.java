package simulator.topology;

import simulator.TreeNode;

/**
 * A generic interface for a factory that generates a simulation topology.
 *
 * @param <C> The type of TopologyConfiguration this factory accepts.
 * @param <R> The type of the root TreeNode this factory produces.
 */
public interface TopologyFactory<C extends TopologyConfiguration, R extends TreeNode> {
    
    /**
     * Generates a topology based on the provided configuration.
     *
     * @param config The configuration object containing parameters for topology generation.
     * @return The root node of the generated topology.
     */
    R generateTopology(C config);
}