package simulator.simulations.performance;

import java.util.logging.Level;
import java.util.logging.Logger;

import simulator.population.ProportionalPublishersPlacement;
import simulator.population.PublishersPlacementStrategy;
import simulator.topology.factories.RegionBrokerFactory;
import simulator.topology.random.RandomTopologyGenerator;
import simulator.topology.random.RegionRandomTopologyConfiguration;
import utils.CustomLogger;

public class RandomTopologyRegionPerformanceSimulation
        extends AbstractRegionPerformanceSimulation<RegionRandomTopologyConfiguration, RandomTopologyGenerator> {

    private static final Logger logger = CustomLogger.getLogger(RandomTopologyRegionPerformanceSimulation.class.getName());

    public RandomTopologyRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
            double subscriptionRegionSize, double remoteInterestProbability,
            boolean enableCsvOutput) {
        super(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability,
                enableCsvOutput);
    }

    // Legacy constructor
    public RandomTopologyRegionPerformanceSimulation(int numberOfReplicas, int subscribersPerReplica,
            double subscriptionRegionSize, double remoteInterestProbability) {
        this(numberOfReplicas, subscribersPerReplica, subscriptionRegionSize, remoteInterestProbability, false);
    }
    
    /**
     * Implements the abstract method to provide the publisher placement strategy.
     * For a random topology, we use a proportional (uniform) distribution.
     */
    @Override
    protected PublishersPlacementStrategy getPublisherPlacementStrategy() {
        // This will result in a uniform random placement, as all leaf
        // brokers have the same MOCK_POPULATION
        return new ProportionalPublishersPlacement();
    }


    public static void main(String[] args) {
        logger.info("--- Starting Random Topology Performance Simulation (Region-Based) ---");

        // --- EASILY ADJUSTABLE PARAMETERS ---

        // Client Parameters
        int simNumberOfReplicas = 10;
        int simSubscribersPerReplica = 1000;

        // Spatial Parameters
        double simWorldWidth = 100.0; // The world is 100 units wide
        double simWorldHeight = 100.0; // The world is 100 units tall

        // This is now an ABSOLUTE size (e.g., 10.0 for a 10x10 region)
        double simSubscriptionRegionSize = 40.0;

        double simRemoteInterestProbability = 0.0;

        // --- CONTROL FLAGS ---
        boolean enableVerboseLogs = true; // Set to true to see debug placement logs
        boolean enableCsvOutput = true; // Set to true to save raw data to files

        // --- Topology Parameters ---
        int topologyTreeDepth = 4;
        int topologyMaxBranching = 3;
        int topologyNumRegions = 5;

        // --- Create Configuration ---
        RegionRandomTopologyConfiguration config = new RegionRandomTopologyConfiguration(
                topologyTreeDepth, topologyMaxBranching, topologyNumRegions,
                0, 0, // Subscribers/Publishers per leaf (handled by populator)
                simWorldWidth, simWorldHeight
        );

        RandomTopologyGenerator factory = new RandomTopologyGenerator(new RegionBrokerFactory());

        RandomTopologyRegionPerformanceSimulation simulation = new RandomTopologyRegionPerformanceSimulation(
                simNumberOfReplicas, simSubscribersPerReplica,
                simSubscriptionRegionSize,
                simRemoteInterestProbability,
                enableCsvOutput);

        if (enableVerboseLogs) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled. Writing FINE logs to: " + CustomLogger.getLogFilePath() + " ---");
        }

        simulation.run(factory, config);
    }
}