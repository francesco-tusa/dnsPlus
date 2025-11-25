package simulator.simulations.performance;

import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyGenerator;

/**
 * Abstract base class for Location-Based simulations using GeoNames.
 */
public abstract class GeoNamesBasedLocationPerformanceSimulation
        extends AbstractLocationPerformanceSimulation<GeoNamesTopologyConfiguration, GeoNamesTopologyGenerator> {

    public void run() {
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyGenerator factory = new GeoNamesTopologyGenerator(new LocationBrokerFactory());
        super.run(factory, config);
    }
}