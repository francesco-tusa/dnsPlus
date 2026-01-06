package simulator.topology.fixed;

import java.util.logging.Logger;
import simulator.topology.TopologyConfiguration;

/**
 * Base configuration for the Simple Fixed Topology family.
 * Defines the common structure (Root -> 3 Children -> 4 Grandchildren).
 */
public class SimpleFixedTopologyConfiguration implements TopologyConfiguration {
    @Override
    public void logDetails(Logger logger) {
        logger.info("Topology Strategy: Simple Fixed (Base Structure)");
    }
}