package simulator.simulations.performance;

import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyLoader;

/**
 * Abstract base class for Location-Based simulations using GeoNames.
 */
public abstract class GeoNamesBasedLocationPerformanceSimulation
        extends AbstractLocationPerformanceSimulation<GeoNamesTopologyConfiguration, GeoNamesTopologyLoader> {

    public void run() {
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyLoader factory = new GeoNamesTopologyLoader(new LocationBrokerFactory());
        super.run(factory, config);
    }
}