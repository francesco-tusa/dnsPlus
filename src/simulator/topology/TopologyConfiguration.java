package simulator.topology;

import java.util.logging.Logger;

public interface TopologyConfiguration {
    /**
     * Logs the specific configuration details of this topology.
     * @param logger The logger to write to.
     */
    void logDetails(Logger logger);
}