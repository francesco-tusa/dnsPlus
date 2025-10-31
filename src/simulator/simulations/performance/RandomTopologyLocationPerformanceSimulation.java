package simulator.simulations.performance;

import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;

/**
 * A concrete simulation that runs a performance scenario on a
 * dynamically generated, random topology using location-based brokers.
 * Parameters are now set in main().
 */
public class RandomTopologyLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    RegionRandomTopologyConfiguration,
    RandomTopologyGenerator
> {

    /**
     * Constructor for the location-based random simulation.
     * @param numberOfReplicas Total number of publisher replicas.
     * @param subscribersPerReplica Number of subscribers for every replica.
     */
    public RandomTopologyLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        super(numberOfReplicas, subscribersPerReplica); // Pass parameters to new abstract constructor
    }

    // Old constants and overridden getter methods are removed.

    public static void main(String[] args) {
        System.out.println("--- Starting Random Topology Performance Simulation (Location-Based) ---");
        
        // --- EASILY ADJUSTABLE PARAMETERS ---
        int simNumberOfReplicas = 10;       // Number of Replicas
        int simSubscribersPerReplica = 100; // Ratio: 100 subs per replica (Total = 1000)

        // --- Topology Parameters ---
        int topologyTreeDepth = 4;
        int topologyMaxBranching = 3;
        int topologyNumRegions = 5;

        // --- Create Configuration and Factory ---
        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(
            topologyTreeDepth, topologyMaxBranching, topologyNumRegions, 0, 0);
            
        RandomTopologyGenerator factory = new RandomTopologyGenerator(new LocationBrokerFactory());
        
        // --- Create and Run the Simulation ---
        RandomTopologyLocationPerformanceSimulation simulation = new RandomTopologyLocationPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica
        );
        
        simulation.run(factory, config);
    }
}