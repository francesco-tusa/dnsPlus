package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.population.PopulationBasedPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.TopologyPaths;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.FileBasedTopologyConfiguration;
import simulator.topology.geonames.FileBasedTopologyGenerator;
import utils.CustomLogger;

/**
 * A concrete simulation run that uses the GeoNames topology,
 * LOCATION-based routing, and the PopulationBasedPublishersPlacement strategy (Top-N).
 */
public class PopulationGeonamesLocationPerformanceSimulation extends GeoNamesBasedLocationPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(PopulationGeonamesLocationPerformanceSimulation.class.getName());

    private final int poolSize;

    public PopulationGeonamesLocationPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
                                                          boolean enableCsvOutput, int poolSize) {
        super(numberOfReplicas, subscribersPerReplica, enableCsvOutput);
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
        logger.info("--- Starting GeoNames-Based Performance Simulation (Population Location-Based) ---");
        
        int simNumberOfReplicas = 20;
        int simSubscribersPerReplica = 25000;
        int simPlacementPoolSize = 100; // Use Top 100 most populated regions
        
        // --- CONTROL FLAGS ---
        boolean enableVerboseLogs = false; 
        boolean enableCsvOutput = true; 

        FileBasedTopologyConfiguration config = new FileBasedTopologyConfiguration(TopologyPaths.FULL_TOPOLOGY);
        FileBasedTopologyGenerator factory = new FileBasedTopologyGenerator(new LocationBrokerFactory());
        
        PopulationGeonamesLocationPerformanceSimulation simulation = new PopulationGeonamesLocationPerformanceSimulation(
            simNumberOfReplicas, simSubscribersPerReplica,
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