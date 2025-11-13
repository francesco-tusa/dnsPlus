package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;
import simulator.visualisation.SimulationVisualiser;
import utils.CustomLogger;

public class GeoNamesBasedRegionPerformanceSimulation extends AbstractRegionPerformanceSimulation<
    FileBasedTopologyConfiguration,
    FileBasedTopologyGenerator
> {

    private static final Logger logger = CustomLogger.getLogger(GeoNamesBasedRegionPerformanceSimulation.class.getName()); // Get logger

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
        logger.info("--- Starting GeoNames-Based Performance Simulation (Region-Based) ---");

        SimulationVisualiser.getInstance().launch();
        
        int simNumberOfReplicas = 20;
        int simSubscribersPerReplica = 2500;
        
        // This is an ABSOLUTE size (e.g., 1.0 decimal degrees)
        double simSubscriptionRegionSize = 1; 
        
        double simRemoteInterestProbability = 0.0;

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
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }
        
        simulation.run(factory, config);
    }
}