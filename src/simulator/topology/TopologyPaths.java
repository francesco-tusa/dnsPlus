package simulator.topology;

/**
 * Central configuration for Topology JSON file paths.
 * Change the constants here to affect all simulations.
 */
public class TopologyPaths {

    // The subset topology used for fast functional testing/debugging
    public static final String SUBSET_TOPOLOGY = "output/geonames_subset_bangladesh_beijing.json";

    // The full-scale topology used for performance simulations.
    // Change this to "output/geonames_topology_rtree.json" (etc.) to switch strategies.
    public static final String FULL_TOPOLOGY = "output/geonames_topology_political.json";

    // Prevent instantiation
    private TopologyPaths() {}
}