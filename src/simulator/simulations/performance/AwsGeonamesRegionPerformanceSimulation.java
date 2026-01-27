package simulator.simulations.performance;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.population.AmazonAwsPublishersPlacement;
import simulator.population.AwsRegions;
import simulator.population.PublishersPlacementStrategy;
import simulator.regions.BoundedBroker;
import simulator.topology.factories.SpatialMatchBrokerFactory;
import simulator.topology.geonames.GeoNamesTopologyConfiguration;
import simulator.topology.geonames.GeoNamesTopologyLoader;
import utils.CustomLogger;

public class AwsGeonamesRegionPerformanceSimulation extends GeoNamesBasedRegionPerformanceSimulation {

    private static final Logger logger = CustomLogger.getLogger(AwsGeonamesRegionPerformanceSimulation.class.getName());

    @Override
    protected PublishersPlacementStrategy getPublisherPlacementStrategy() {
        return new AmazonAwsPublishersPlacement();
    }
    
    @Override
    protected List<BoundedBroker> getInterestHotspots(BoundedBroker root) {
        // Wire the AWS regions to the Workload Generator
        return AwsRegions.findAwsBrokers(root);
    }

    public static void main(String[] args) {
        logger.info("--- Starting GeoNames-Based Performance Simulation (AWS Region-Based) ---");
        GeoNamesTopologyConfiguration config = new GeoNamesTopologyConfiguration();
        GeoNamesTopologyLoader factory = new GeoNamesTopologyLoader(new SpatialMatchBrokerFactory());
        
        AwsGeonamesRegionPerformanceSimulation simulation = new AwsGeonamesRegionPerformanceSimulation();
        
        if (config.isEnableVerboseLogs()) {
            simulation.setLogLevel(Level.FINE);
            logger.info("--- Verbose logging enabled ---");
        }
        simulation.run(factory, config);
    }
}