package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;

import simulator.population.PopulationBasedPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.factories.LocationBrokerFactory;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import utils.CustomLogger;

public class RandomTopologyLocationPerformanceSimulation extends AbstractLocationPerformanceSimulation<
    RegionRandomTopologyConfiguration,
    RandomTopologyGenerator
> {

    private static final Logger logger = CustomLogger.getLogger(RandomTopologyLocationPerformanceSimulation.class.getName());
    
    /**
     * Implements the abstract method to provide the publisher placement strategy.
     * For a random topology, we use a proportional (uniform) distribution.
     */
    @Override
    protected PublishersPlacementStrategy getPublisherPlacementStrategy() {
        return new PopulationBasedPublishersPlacement();
    }


    public static void main(String[] args) {
        logger.info("--- Starting Random Topology Performance Simulation (Location-Based) ---");

        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration();
        RandomTopologyGenerator factory = new RandomTopologyGenerator(new LocationBrokerFactory());
        RandomTopologyLocationPerformanceSimulation simulation = new RandomTopologyLocationPerformanceSimulation();
        
        if (config.isEnableVerboseLogs()) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }
        
        simulation.run(factory, config);
    }
}