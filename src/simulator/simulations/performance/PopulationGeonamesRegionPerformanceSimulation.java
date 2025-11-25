package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.population.PopulationBasedPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.TopologyPaths;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyGenerator;
import simulator.visualisation.SimulationVisualiser;
import utils.CustomLogger;

/**
 * A concrete simulation run that uses the GeoNames topology and the
 * PopulationBasedPublishersPlacement strategy (Top-N by Pop).
 */
public class PopulationGeonamesRegionPerformanceSimulation extends GeoNamesBasedRegionPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(PopulationGeonamesRegionPerformanceSimulation.class.getName());

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
        logger.info("--- Starting GeoNames-Based Performance Simulation (Top-N Population-Based) ---");

        SimulationVisualiser.getInstance().launch();

        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyGenerator factory = new GeoNamesTopologyGenerator(new SpatialMatchBrokerFactory());
        
        PopulationGeonamesRegionPerformanceSimulation simulation = new PopulationGeonamesRegionPerformanceSimulation();
        
        if (config.isEnableVerboseLogs()) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }
        
        simulation.run(factory, config);
    }
}