package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger; // Import Logger
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import utils.CustomLogger; // Import CustomLogger

public class RandomTopologyLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    RegionRandomTopologyConfiguration,
    RandomTopologyGenerator
> {

    private static final Logger logger = CustomLogger.getLogger(RandomTopologyLocationPerformanceSimulation.class.getName()); // Get logger

    public RandomTopologyLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                       boolean enableCsvOutput) { 
        super(numberOfReplicas, subscribersPerReplica, enableCsvOutput); 
    }
    
    // Legacy constructor
    public RandomTopologyLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica) {
        this(numberOfReplicas, subscribersPerReplica, false);
    }

    public static void main(String[] args) {
        logger.info("--- Starting Random Topology Performance Simulation (Location-Based) ---");
        
        int simNumberOfReplicas = 10;
        int simSubscribersPerReplica = 100;
        
        // --- CONTROL FLAGS ---
        boolean enableVerboseLogs = false; 
        boolean enableCsvOutput = true; 

        int topologyTreeDepth = 4;
        int topologyMaxBranching = 3;
        int topologyNumRegions = 5;
        
        // Spatial Parameters (Location-based sim doesn't use sub region size)
        double simWorldWidth = 100.0;
        double simWorldHeight = 100.0; 

        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(
            topologyTreeDepth, topologyMaxBranching, topologyNumRegions, 
            0, 0,
            simWorldWidth, simWorldHeight
        );
            
        RandomTopologyGenerator factory = new RandomTopologyGenerator(new LocationBrokerFactory());
        
        RandomTopologyLocationPerformanceSimulation simulation = new RandomTopologyLocationPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica,
            enableCsvOutput 
        );
        
        if (enableVerboseLogs) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }
        
        simulation.run(factory, config);
    }
}