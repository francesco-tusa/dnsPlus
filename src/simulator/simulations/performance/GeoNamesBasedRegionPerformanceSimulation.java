package simulator.simulations.performance;

import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyGenerator;

/**
 * Abstract base class for Region-Based simulations using GeoNames.
 * Centralizes the topology loading logic using the default Full Topology.
 */
public abstract class GeoNamesBasedRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    GeoNamesTopologyConfiguration,
    GeoNamesTopologyGenerator
> {

    
    public void run() {
        // Config automatically loads all params from SimConfiguration
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyGenerator factory = new GeoNamesTopologyGenerator(new SpatialMatchBrokerFactory());
        super.run(factory, config);
    }
}