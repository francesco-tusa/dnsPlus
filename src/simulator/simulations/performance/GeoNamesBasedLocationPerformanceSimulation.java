package simulator.simulations.performance;

import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

/**
 * A concrete simulation that runs a performance scenario on the
 * full, data-driven topology generated from GeoNames data using location-based brokers.
 * Parameters are now set in main().
 */
public class GeoNamesBasedLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    /**
     * Constructor for the location-based GeoNames simulation.
     * @param numberOfReplicas Total number of publisher replicas.
     * @param subscribersPerReplica Number of subscribers for every replica.
     */
    public GeoNamesBasedLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        super(numberOfReplicas, subscribersPerReplica); // Pass parameters to new abstract constructor
    }

    // Old constants and overridden getter methods are removed.

    public static void main(String[] args) {
        System.out.println("--- Starting GeoNames-Based Performance Simulation (Location-Based) ---");
        
        // --- EASILY ADJUSTABLE PARAMETERS ---
        int simNumberOfReplicas = 20;         // Number of Replicas
        int simSubscribersPerReplica = 25000; // Ratio: 25k subs per replica (Total = 500,000)

        // --- Create Configuration and Factory ---
        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration("output/geonames_topology.json");
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new LocationBrokerFactory());
        
        // --- Create and Run the Simulation ---
        GeoNamesBasedLocationPerformanceSimulation simulation = new GeoNamesBasedLocationPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica
        );
        
        simulation.run(factory, config);
    }
}