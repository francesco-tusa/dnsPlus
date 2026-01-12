package simulator;

import java.util.logging.Logger;

import simulator.simulations.performance.AwsGeonamesLocationPerformanceSimulation;
import utils.CustomLogger;

/**
 * Main entry point for all PERFORMANCE simulations using LOCATION-BASED routing brokers.
 */
public class LocationPerformanceSimulationsMain {

    private static final Logger logger = CustomLogger.getLogger(LocationPerformanceSimulationsMain.class.getName());

    public static void main(String[] args) {
        logger.info("--- Launching Location-Based GeoNames Topology Simulation ---");
        AwsGeonamesLocationPerformanceSimulation.main(args);

    }
}
