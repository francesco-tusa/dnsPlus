package simulator.topology.factories;

import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SpatialMatchLeafBroker;

/**
 * A factory for creating brokers that use the region-based routing strategy.
 */
public class RegionBrokerFactory implements BrokerFactory {
    @Override
    public BoundedBroker createBroker(String name) {
        return new SpatialMatchBroker(name);
    }

    @Override
    public BoundedBroker createLeafBroker(String name, Location p1, Location p2) {
        return new SpatialMatchLeafBroker(name, p1, p2);
    }

    @Override
    public BoundedBroker createLeafBroker(String name) {
        return new SpatialMatchLeafBroker(name);
    }
}