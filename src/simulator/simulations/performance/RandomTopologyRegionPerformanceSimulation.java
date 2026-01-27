package simulator.simulations.performance;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import simulator.population.PopulationBasedPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.regions.BoundedBroker;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import utils.CustomLogger;

public class RandomTopologyRegionPerformanceSimulation
        extends AbstractRegionPerformanceSimulation<RegionRandomTopologyConfiguration, RandomTopologyGenerator> {

    private static final Logger logger = CustomLogger.getLogger(RandomTopologyRegionPerformanceSimulation.class.getName());

    
    /**
     * Implements the abstract method to provide the publisher placement strategy.
     * For a random topology, we use a proportional (uniform) distribution.
     */
    @Override
    protected PublishersPlacementStrategy getPublisherPlacementStrategy() {
        return new PopulationBasedPublishersPlacement();
    }


    public static void main(String[] args) {
        logger.info("--- Starting Random Topology Performance Simulation (Region-Based) ---");

        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration();
        RandomTopologyGenerator factory = new RandomTopologyGenerator(new SpatialMatchBrokerFactory());
        RandomTopologyRegionPerformanceSimulation simulation = new RandomTopologyRegionPerformanceSimulation();

        if (config.isEnableVerboseLogs()) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }

        simulation.run(factory, config);
    }


    @Override
    protected List<BoundedBroker> getInterestHotspots(BoundedBroker root) {
        // Wire Top-Pop brokers to the Workload Generator
        List<BoundedBroker> leafBrokers = TopologyAnalyser.findLeafBrokers(root);
        return leafBrokers.stream()
            .sorted(Comparator.comparingLong(BoundedBroker::getInternetPopulation).reversed())
            .limit(100) // we reuse same value as for the Geonames simulation
            .collect(Collectors.toList());
    }
}