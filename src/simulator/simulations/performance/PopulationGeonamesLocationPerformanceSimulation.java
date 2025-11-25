package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.population.PopulationBasedPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.TopologyPaths;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyGenerator;
import utils.CustomLogger;

/**
 * A concrete simulation run that uses the GeoNames topology,
 * LOCATION-based routing, and the PopulationBasedPublishersPlacement strategy (Top-N).
 */
public class PopulationGeonamesLocationPerformanceSimulation extends GeoNamesBasedLocationPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(PopulationGeonamesLocationPerformanceSimulation.class.getName());

    private final int poolSize = 100;

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

        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyGenerator factory = new GeoNamesTopologyGenerator(new LocationBrokerFactory());
        
        PopulationGeonamesLocationPerformanceSimulation simulation = new PopulationGeonamesLocationPerformanceSimulation();
        
        if (config.isEnableVerboseLogs()) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }
        
        simulation.run(factory, config);
    }
}