package simulator.topology.fixed;

import java.util.logging.Logger;

public class SimpleCoordinateFixedTopologyConfiguration extends SimpleFixedTopologyConfiguration {
    @Override
    public void logDetails(Logger logger) {
        logger.info("Topology Strategy: Simple Fixed (Coordinate/Routing Mode)");
    }
}