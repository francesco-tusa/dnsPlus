package simulator.simulations.performance;

import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

/**
 * A concrete simulation that runs a performance scenario on the
 * full, data-driven topology generated from GeoNames data using location-based brokers.
 */
public class GeoNamesBasedLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    // --- Simulation Parameters ---
    private static final long TOTAL_SUBSCRIBERS = 500_000;
    private static final int NUMBER_OF_REPLICAS = 20;
    private static final double REMOTE_INTEREST_PROBABILITY = 0.1;

    @Override
    protected long getTotalSubscribers() { return TOTAL_SUBSCRIBERS; }

    @Override
    protected int getNumberOfReplicas() { return NUMBER_OF_REPLICAS; }
    
    @Override
    protected double getRemoteInterestProbability() { return REMOTE_INTEREST_PROBABILITY; }

    public static void main(String[] args) {
        System.out.println("--- Starting GeoNames-Based Performance Simulation (Location-Based) ---");
        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration("output/geonames_topology.json");
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new LocationBrokerFactory());
        GeoNamesBasedLocationPerformanceSimulation simulation = new GeoNamesBasedLocationPerformanceSimulation();
        simulation.run(factory, config);
    }
}