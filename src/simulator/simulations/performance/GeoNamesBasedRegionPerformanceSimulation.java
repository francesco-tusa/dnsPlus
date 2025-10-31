package simulator.simulations.performance;

import java.util.logging.Level; // Import Level
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

public class GeoNamesBasedRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    public GeoNamesBasedRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                    double subscriptionRegionSize, double remoteInterestProbability) {
        super(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability);
    }
    
    public static void main(String[] args) {
        System.out.println("--- Starting GeoNames-Based Performance Simulation (Region-Based) ---");
        
        int simNumberOfReplicas = 20;
        int simSubscribersPerReplica = 2500;
        double simSubscriptionRegionSize = 1.0;
        double simRemoteInterestProbability = 0.1;

        // --- New logging flag ---
        boolean enableVerboseLogs = true; // Set to true to see debug placement logs

        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration("output/geonames_topology.json");
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new RegionBrokerFactory());
        
        GeoNamesBasedRegionPerformanceSimulation simulation = new GeoNamesBasedRegionPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica, simSubscriptionRegionSize, simRemoteInterestProbability
        );
        
        // --- Use the new setter to enable verbose logs ---
        if (enableVerboseLogs) {
            simulation.setLogLevel(Level.FINE);
            System.out.println("--- VERBOSE LOGGING ENABLED (Level.FINE) ---");
        }
        
        simulation.run(factory, config);
    }
}