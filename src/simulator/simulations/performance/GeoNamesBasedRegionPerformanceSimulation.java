package simulator.simulations.performance;

import java.util.logging.Level;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;

public class GeoNamesBasedRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    public GeoNamesBasedRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                    double subscriptionRegionSize, double remoteInterestProbability,
                                                    boolean enableCsvOutput) {
        super(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability, enableCsvOutput);
    }

    // Legacy constructor
    public GeoNamesBasedRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                    double subscriptionRegionSize, double remoteInterestProbability) {
        this(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability, false);
    }
    
    public static void main(String[] args) {
        System.out.println("--- Starting GeoNames-Based Performance Simulation (Region-Based) ---");
        
        int simNumberOfReplicas = 20;
        int simSubscribersPerReplica = 2500;
        
        // This is an ABSOLUTE size (e.g., 1.0 decimal degrees)
        double simSubscriptionRegionSize = 1.0; 
        
        double simRemoteInterestProbability = 0.1;

        // --- CONTROL FLAGS ---
        boolean enableVerboseLogs = false;
        boolean enableCsvOutput = true;

        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration("output/geonames_topology.json");
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new RegionBrokerFactory());
        
        GeoNamesBasedRegionPerformanceSimulation simulation = new GeoNamesBasedRegionPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica, 
            simSubscriptionRegionSize, // Pass the absolute size
            simRemoteInterestProbability,
            enableCsvOutput
        );
        
        if (enableVerboseLogs) {
            simulation.setLogLevel(Level.FINE);
        }
        
        simulation.run(factory, config);
    }
}