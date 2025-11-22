package simulator.topology.geonames.builder;

public interface TopologyBuilderStrategy {
    /**
     * Builds the broker hierarchy.
     */
    GeoNamesBuilderNode build(GeoNamesDataLoader loader);

    /**
     * Creates a subset of the topology (e.g., Bangladesh + Beijing) for testing.
     * Strategies implement this based on their specific tree structure.
     */
    GeoNamesBuilderNode createSubset(GeoNamesBuilderNode root);

    /**
     * Returns the full output file path for the Full Topology.
     */
    String getOutputFilePath();
    
    /**
     * Returns the full output file path for the Subset Topology.
     */
    String getSubsetOutputFilePath();
}