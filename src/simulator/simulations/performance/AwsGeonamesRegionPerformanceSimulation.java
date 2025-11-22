package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.population.AmazonAwsPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.TopologyPaths;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;
import simulator.visualisation.SimulationVisualiser;
import utils.CustomLogger;

/**
 * A concrete simulation run that uses the GeoNames topology and the
 * AmazonAwsPublishersPlacement strategy.
 */
public class AwsGeonamesRegionPerformanceSimulation extends GeoNamesBasedRegionPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(AwsGeonamesRegionPerformanceSimulation.class.getName());

    public AwsGeonamesRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                          double subscriptionRegionSize, double remoteInterestProbability,
                                          boolean enableCsvOutput) {
        super(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability, enableCsvOutput);
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
        logger.info("--- Starting GeoNames-Based Performance Simulation (AWS Region-Based) ---");

        SimulationVisualiser.getInstance().launch();
        
        int simNumberOfReplicas = 20;
        int simSubscribersPerReplica = 2500;
        
        double simSubscriptionRegionSize = 1; 
        double simRemoteInterestProbability = 0.0;

        boolean enableVerboseLogs = false;
        boolean enableCsvOutput = true;

        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration(TopologyPaths.FULL_TOPOLOGY);
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new RegionBrokerFactory());
        
        AwsGeonamesRegionPerformanceSimulation simulation = new AwsGeonamesRegionPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica, 
            simSubscriptionRegionSize, 
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