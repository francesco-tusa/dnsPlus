package simulator.topology.geonames;

import simulator.topology.TopologyConfiguration;

/**
 * Configuration class for topologies loaded from a file.
 * Specifies the path to the JSON file containing the topology definition.
 */
public class FileBasedTopologyConfiguration implements TopologyConfiguration {

    private final String topologyFilePath;

    /**
     * Constructor for FileBasedTopologyConfiguration.
     *
     * @param topologyFilePath The path to the JSON file defining the topology.
     */
    public FileBasedTopologyConfiguration(String topologyFilePath) {
        if (topologyFilePath == null || topologyFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Topology file path cannot be null or empty.");
        }
        this.topologyFilePath = topologyFilePath;
    }

    /**
     * Gets the path to the topology JSON file.
     *
     * @return The file path.
     */
    public String getTopologyFilePath() {
        return topologyFilePath;
    }

    @Override
    public String toString() {
        return "FileBasedTopologyConfiguration{" +
               "topologyFilePath='" + topologyFilePath + '\'' +
               '}';
    }
}

