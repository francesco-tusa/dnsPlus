package simulator.simulations.performance;

import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

/**
 * A concrete simulation that runs a performance scenario on the
 * full, data-driven topology generated from GeoNames data using region-based brokers.
 */
public class GeoNamesBasedRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    // --- Simulation Parameters ---
    private static final long TOTAL_SUBSCRIBERS = 50_000;
    private static final int NUMBER_OF_REPLICAS = 20;
    private static final double SUBSCRIPTION_REGION_SIZE = 1.0;
    private static final double REMOTE_INTEREST_PROBABILITY = 0.1;

    @Override
    protected long getTotalSubscribers() { return TOTAL_SUBSCRIBERS; }

    @Override
    protected int getNumberOfReplicas() { return NUMBER_OF_REPLICAS; }
    
    @Override
    protected double getRemoteInterestProbability() { return REMOTE_INTEREST_PROBABILITY; }

    @Override
    protected double getSubscriptionRegionSize() { return SUBSCRIPTION_REGION_SIZE; }

    public static void main(String[] args) {
        System.out.println("--- Starting GeoNames-Based Performance Simulation (Region-Based) ---");
        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration("output/geonames_topology.json");
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new RegionBrokerFactory());
        GeoNamesBasedRegionPerformanceSimulation simulation = new GeoNamesBasedRegionPerformanceSimulation();
        simulation.run(factory, config);
    }
}