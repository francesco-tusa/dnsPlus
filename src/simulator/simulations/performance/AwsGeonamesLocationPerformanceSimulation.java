package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.population.AmazonAwsPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.TopologyPaths;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;
import utils.CustomLogger;

/**
 * A concrete simulation run that uses the GeoNames topology,
 * LOCATION-based routing, and the AmazonAwsPublishersPlacement strategy.
 */
public class AwsGeonamesLocationPerformanceSimulation extends GeoNamesBasedLocationPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(AwsGeonamesLocationPerformanceSimulation.class.getName());

    public AwsGeonamesLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                    boolean enableCsvOutput) {
        super(numberOfReplicas, subscribersPerReplica, enableCsvOutput);
    }

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
        
        int simNumberOfReplicas = 20;
        int simSubscribersPerReplica = 25000;
        
        // --- CONTROL FLAGS ---
        boolean enableVerboseLogs = false; 
        boolean enableCsvOutput = true; 

        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration(TopologyPaths.FULL_TOPOLOGY);
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new LocationBrokerFactory());
        
        AwsGeonamesLocationPerformanceSimulation simulation = new AwsGeonamesLocationPerformanceSimulation(
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