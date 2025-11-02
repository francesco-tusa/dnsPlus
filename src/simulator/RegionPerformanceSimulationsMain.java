package simulator;

import java.util.logging.Logger;
import simulator.simulations.performance.GeoNamesBasedRegionPerformanceSimulation;
import simulator.simulations.performance.RandomTopologyRegionPerformanceSimulation;
import utils.CustomLogger;

/**
 * Main entry point for all PERFORMANCE simulations using REGION-BASED routing brokers.
 */
public class RegionPerformanceSimulationsMain {
    
    private static final Logger logger = CustomLogger.getLogger(RegionPerformanceSimulationsMain.class.getName());

    public static void main(String[] args) {
        // Run a smaller-scale performance test on a random topology
        logger.info("--- Launching Region-Based Random Topology Simulation ---");
        RandomTopologyRegionPerformanceSimulation.main(args);
        
        // Uncomment the line below to run the full-scale, realistic performance test
        // logger.info("--- Launching Region-Based GeoNames Topology Simulation ---");
        // GeoNamesBasedRegionPerformanceSimulation.main(args);
    }
}
