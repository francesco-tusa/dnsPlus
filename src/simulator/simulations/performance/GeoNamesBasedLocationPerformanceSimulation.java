package simulator.simulations.performance;

import java.util.logging.Level;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

public class GeoNamesBasedLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    public GeoNamesBasedLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                      boolean enableCsvOutput) { 
        super(numberOfReplicas, subscribersPerReplica, enableCsvOutput); 
    }
    
    // Legacy constructor
    public GeoNamesBasedLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        this(numberOfReplicas, subscribersPerReplica, false);
    }

    public static void main(String[] args) {
        System.out.println("--- Starting GeoNames-Based Performance Simulation (Location-Based) ---");
        
        int simNumberOfReplicas = 20;
        int simSubscribersPerReplica = 25000;
        
        // --- CONTROL FLAGS ---
        boolean enableVerboseLogs = false; 
        boolean enableCsvOutput = true; 

        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration("output/geonames_topology.json");
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new LocationBrokerFactory());
        
        GeoNamesBasedLocationPerformanceSimulation simulation = new GeoNamesBasedLocationPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica,
            enableCsvOutput 
        );
        
        if (enableVerboseLogs) {
            simulation.setLogLevel(Level.FINE);
        }
        
        simulation.run(factory, config);
    }
}