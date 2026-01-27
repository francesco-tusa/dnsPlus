package simulator;

import java.util.logging.Logger;

import simulator.simulations.performance.AwsGeonamesRegionPerformanceSimulation;
import simulator.simulations.performance.PopulationGeonamesRegionPerformanceSimulation;
import utils.CustomLogger;

/**
 * Main entry point for all PERFORMANCE simulations using REGION-BASED routing
 * brokers.
 */
public class RegionPerformanceSimulationsMain {

    private static final Logger logger = CustomLogger.getLogger(RegionPerformanceSimulationsMain.class.getName());

    public static void main(String[] args) {
        // --- CHOOSE WHICH SIMULATION TO RUN ---

        // Option 1: Run the simulation using the curated AWS region list
        // logger.info("--- Launching Region-Based GeoNames Simulation (AWS Strategy) ---");
        // AwsGeonamesRegionPerformanceSimulation.main(args);

        // Option 2: Run the simulation using Population-Density Placement
        // (Note: If remote subscriptions are enabled, they will target the Top-100 Populated Cities)
        logger.info("--- Launching Region-Based GeoNames Simulation (Population Strategy) ---");
        PopulationGeonamesRegionPerformanceSimulation.main(args);
    }
}