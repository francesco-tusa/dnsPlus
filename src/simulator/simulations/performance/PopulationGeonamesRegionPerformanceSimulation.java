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
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyLoader;
import utils.CustomLogger;

public class PopulationGeonamesRegionPerformanceSimulation extends GeoNamesBasedRegionPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(PopulationGeonamesRegionPerformanceSimulation.class.getName());
    private final int poolSize = 100; 

    @Override
    protected PublishersPlacementStrategy getPublisherPlacementStrategy() {
        return new PopulationBasedPublishersPlacement(this.poolSize);
    }
    
    @Override
    protected List<BoundedBroker> getInterestHotspots(BoundedBroker root) {
        // Wire Top-Pop brokers to the Workload Generator
        List<BoundedBroker> leafBrokers = TopologyAnalyser.findLeafBrokers(root);
        return leafBrokers.stream()
            .sorted(Comparator.comparingLong(BoundedBroker::getInternetPopulation).reversed())
            .limit(poolSize)
            .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        logger.info("--- Starting GeoNames-Based Performance Simulation (Top-N Population-Based) ---");
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyLoader factory = new GeoNamesTopologyLoader(new SpatialMatchBrokerFactory());
        
        PopulationGeonamesRegionPerformanceSimulation simulation = new PopulationGeonamesRegionPerformanceSimulation();
        
        if (config.isEnableVerboseLogs()) {
            simulation.setLogLevel(Level.FINE);
        }
        simulation.run(factory, config);
    }
}