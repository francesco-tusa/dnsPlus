package simulator.topology.fixed;

import java.util.logging.Logger;

public class SimpleRegionFixedTopologyConfiguration extends SimpleFixedTopologyConfiguration {
    @Override
    public void logDetails(Logger logger) {
        logger.info("Topology Strategy: Simple Fixed (Region/Spatial Mode)");
    }
}