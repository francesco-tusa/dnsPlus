package simulator;

import java.util.logging.Logger;

import simulator.simulations.performance.AwsGeonamesLocationPerformanceSimulation;
import simulator.simulations.performance.PopulationGeonamesLocationPerformanceSimulation;
import utils.CustomLogger;

/**
 * Main entry point for all PERFORMANCE simulations using LOCATION-BASED routing brokers.
 */
public class LocationPerformanceSimulationsMain {

    private static final Logger logger = CustomLogger.getLogger(LocationPerformanceSimulationsMain.class.getName());

    public static void main(String[] args) {

        // Option 1: Run the simulation using the AWS Strategy
        logger.info("--- Launching Location-Based GeoNames Simulation (AWS Strategy) ---");
        AwsGeonamesLocationPerformanceSimulation.main(args);

        // Option 2: Run the simulation using the Population Strategy
        // logger.info("--- Launching Location-Based GeoNames Simulation (Population Strategy) ---");
        // PopulationGeonamesLocationPerformanceSimulation.main(args);
    }
}