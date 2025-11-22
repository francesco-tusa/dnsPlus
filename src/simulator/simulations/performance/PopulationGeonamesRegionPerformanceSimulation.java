package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.population.PopulationBasedPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.TopologyPaths;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;
import simulator.visualisation.SimulationVisualiser;
import utils.CustomLogger;

/**
 * A concrete simulation run that uses the GeoNames topology and the
 * PopulationBasedPublishersPlacement strategy (Top-N by Pop).
 */
public class PopulationGeonamesRegionPerformanceSimulation extends GeoNamesBasedRegionPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(PopulationGeonamesRegionPerformanceSimulation.class.getName());

    // The "N" for the "Top-N" pool
    private final int poolSize; 

    public PopulationGeonamesRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                 double subscriptionRegionSize, double remoteInterestProbability,
                                                 boolean enableCsvOutput, int poolSize) {
        super(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability, enableCsvOutput);
        this.poolSize = poolSize;
    }

    /**
     * Implements the abstract method to provide the Population-based strategy.
     */
    @Override
    protected PublishersPlacementStrategy getPublisherPlacementStrategy() {
        return new PopulationBasedPublishersPlacement(this.poolSize);
    }

    /**
     * Main entry point for this specific simulation.
     */
    public static void main(String[] args) {
        logger.info("--- Starting GeoNames-Based Performance Simulation (Top-N Population-Based) ---");

        SimulationVisualiser.getInstance().launch();
        
        int simNumberOfReplicas = 20;
        int simSubscribersPerReplica = 2500;
        int simPlacementPoolSize = 100; // Use Top 100 most populated regions
        
        double simSubscriptionRegionSize = 1; 
        double simRemoteInterestProbability = 0.0;

        boolean enableVerboseLogs = false;
        boolean enableCsvOutput = true;

        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration(TopologyPaths.FULL_TOPOLOGY);
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new RegionBrokerFactory());
        
        PopulationGeonamesRegionPerformanceSimulation simulation = new PopulationGeonamesRegionPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica, 
            simSubscriptionRegionSize, 
            simRemoteInterestProbability,
            enableCsvOutput,
            simPlacementPoolSize
        );
        
        if (enableVerboseLogs) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }
        
        simulation.run(factory, config);
    }
}