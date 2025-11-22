package simulator;

import java.util.logging.Logger;

import simulator.simulations.performance.AwsGeonamesLocationPerformanceSimulation;
import simulator.simulations.performance.RandomTopologyLocationPerformanceSimulation;
import utils.CustomLogger;

/**
 * Main entry point for all PERFORMANCE simulations using LOCATION-BASED routing brokers.
 */
public class LocationPerformanceSimulationsMain {

    private static final Logger logger = CustomLogger.getLogger(LocationPerformanceSimulationsMain.class.getName());

    public static void main(String[] args) {
        // Run a smaller-scale performance test on a random topology
        logger.info("--- Launching Location-Based Random Topology Simulation ---");
        RandomTopologyLocationPerformanceSimulation.main(args);
        
        // Uncomment the line below to run the full-scale, realistic performance test
        // logger.info("--- Launching Location-Based GeoNames Topology Simulation ---");
        AwsGeonamesLocationPerformanceSimulation.main(args);

    }
}
