package simulator.simulations.performance;

import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;

/**
 * A concrete simulation that runs a performance scenario on a
 * dynamically generated, random topology using region-based brokers.
 */
public class RandomTopologyRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    RegionRandomTopologyConfiguration,
    RandomTopologyGenerator
> {

    // --- Simulation Parameters ---
    private static final long TOTAL_SUBSCRIBERS = 1000;
    private static final int NUMBER_OF_REPLICAS = 10;
    private static final double SUBSCRIPTION_REGION_SIZE = 10.0;
    private static final double REMOTE_INTEREST_PROBABILITY = 0.2;
    
    // --- Topology Parameters ---
    private static final int TREE_DEPTH = 4;
    private static final int MAX_BRANCHING = 3;
    private static final int NUM_REGIONS = 5;

    @Override
    protected long getTotalSubscribers() { return TOTAL_SUBSCRIBERS; }

    @Override
    protected int getNumberOfReplicas() { return NUMBER_OF_REPLICAS; }
    
    @Override
    protected double getRemoteInterestProbability() { return REMOTE_INTEREST_PROBABILITY; }

    @Override
    protected double getSubscriptionRegionSize() { return SUBSCRIPTION_REGION_SIZE; }

    public static void main(String[] args) {
        System.out.println("--- Starting Random Topology Performance Simulation (Region-Based) ---");
        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(TREE_DEPTH, MAX_BRANCHING, NUM_REGIONS, 0, 0);
        RandomTopologyGenerator factory = new RandomTopologyGenerator(new RegionBrokerFactory());
        RandomTopologyRegionPerformanceSimulation simulation = new RandomTopologyRegionPerformanceSimulation();
        simulation.run(factory, config);
    }
}