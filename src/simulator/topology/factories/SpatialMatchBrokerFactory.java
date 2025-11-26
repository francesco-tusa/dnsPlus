package simulator.topology.factories;

import simulator.config.BrokerConfig;
import simulator.config.SimConfiguration;
import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SpatialMatchLeafBroker;

public class SpatialMatchBrokerFactory implements BoundedBrokerFactory {

    private boolean forceSingleRegion; 
    private double smartThreshold;

    public SpatialMatchBrokerFactory() {
        SimConfiguration config = SimConfiguration.get();
        
        // Map configuration StrategyType to the boolean flag used by SpatialMatchBroker
        // SIMPLE -> forceSingleRegion = true (Legacy Aggregation)
        // SMART  -> forceSingleRegion = false (Multi-Region Threshold Aggregation)
        this.forceSingleRegion = (config.broker.strategy == BrokerConfig.StrategyType.SIMPLE);
        this.smartThreshold = config.broker.smartThreshold;
    }
    
    public SpatialMatchBrokerFactory(boolean forceSingleRegion, double threshold) {
        this.forceSingleRegion = forceSingleRegion;
        this.smartThreshold = threshold;
    }

    @Override
    public BoundedBroker createBroker(String name) {
        return new SpatialMatchBroker(name, forceSingleRegion, smartThreshold);
    }

    @Override
    public BoundedBroker createLeafBroker(String name, Location p1, Location p2) {
        return new SpatialMatchLeafBroker(name, p1, p2, forceSingleRegion, smartThreshold);
    }

    @Override
    public BoundedBroker createLeafBroker(String name) {
        return new SpatialMatchLeafBroker(name, forceSingleRegion, smartThreshold);
    }
}