package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.population.AmazonAwsPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyGenerator;
import utils.CustomLogger;

/**
 * A concrete simulation run that uses the GeoNames topology,
 * LOCATION-based routing, and the AmazonAwsPublishersPlacement strategy.
 */
public class AwsGeonamesLocationPerformanceSimulation extends GeoNamesBasedLocationPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(AwsGeonamesLocationPerformanceSimulation.class.getName());


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
        logger.info("--- Starting GeoNames-Based Performance Simulation (AWS Location-Based) ---");

        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyGenerator factory = new GeoNamesTopologyGenerator(new LocationBrokerFactory());
        
        AwsGeonamesLocationPerformanceSimulation simulation = new AwsGeonamesLocationPerformanceSimulation();
        
        if (config.isEnableVerboseLogs()) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }
        
        simulation.run(factory, config);
    }
}