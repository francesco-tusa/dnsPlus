package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.population.AmazonAwsPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyGenerator;
import simulator.visualisation.SimulationVisualiser;
import utils.CustomLogger;

/**
 * A concrete simulation run that uses the GeoNames topology and the
 * AmazonAwsPublishersPlacement strategy.
 */
public class AwsGeonamesRegionPerformanceSimulation extends GeoNamesBasedRegionPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(AwsGeonamesRegionPerformanceSimulation.class.getName());

    /**
     * Implements the abstract method to provide the AWS-based strategy.
     */
    @Override
    protected PublishersPlacementStrategy getPublisherPlacementStrategy() {
        return new AmazonAwsPublishersPlacement();
    }

    /**
     * Main entry point for this specific simulation.
     */
    public static void main(String[] args) {
        logger.info("--- Starting GeoNames-Based Performance Simulation (AWS Region-Based) ---");

        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyGenerator factory = new GeoNamesTopologyGenerator(new SpatialMatchBrokerFactory());
        
        AwsGeonamesRegionPerformanceSimulation simulation = new AwsGeonamesRegionPerformanceSimulation();
        
        if (config.isEnableVerboseLogs()) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }
        
        simulation.run(factory, config);
    }
}