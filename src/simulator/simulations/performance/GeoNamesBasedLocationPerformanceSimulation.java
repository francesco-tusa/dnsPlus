package simulator.simulations.performance;

import java.util.logging.Level; // Import Level
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

public class GeoNamesBasedLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    public GeoNamesBasedLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        super(numberOfReplicas, subscribersPerReplica);
    }

    public static void main(String[] args) {
        System.out.println("--- Starting GeoNames-Based Performance Simulation (Location-Based) ---");
        
        int simNumberOfReplicas = 20;
        int simSubscribersPerReplica = 25000;
        
        // --- New logging flag ---
        boolean enableVerboseLogs = true; // Set to true to see debug placement logs

        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration("output/geonames_topology.json");
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new LocationBrokerFactory());
        
        GeoNamesBasedLocationPerformanceSimulation simulation = new GeoNamesBasedLocationPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica
        );
        
        // --- Use the new setter to enable verbose logs ---
        if (enableVerboseLogs) {
            simulation.setLogLevel(Level.FINE);
            System.out.println("--- VERBOSE LOGGING ENABLED (Level.FINE) ---");
        }
        
        simulation.run(factory, config);
    }
}