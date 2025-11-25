package simulator.topology.factories;

import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SpatialMatchLeafBroker;

public class SpatialMatchBrokerFactory implements BoundedBrokerFactory {

    // Default to Legacy Mode (Always Aggregate) unless specified
    private boolean forceSingleRegion = true; 
    private double smartThreshold = 0.5;

    public SpatialMatchBrokerFactory() {}
    
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