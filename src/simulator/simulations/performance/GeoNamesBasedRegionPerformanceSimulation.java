package simulator.simulations.performance;

import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyLoader;

/**
 * Abstract base class for Region-Based simulations using GeoNames.
 * Centralizes the topology loading logic using the default Full Topology.
 */
public abstract class GeoNamesBasedRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    GeoNamesTopologyConfiguration,
    GeoNamesTopologyLoader
> {

    
    public void run() {
        // Config automatically loads all params from SimConfiguration
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyLoader factory = new GeoNamesTopologyLoader(new SpatialMatchBrokerFactory());
        super.run(factory, config);
    }
}