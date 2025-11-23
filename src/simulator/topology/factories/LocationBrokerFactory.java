package simulator.topology.factories;

import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.regions.ProximityRoutingBroker;
import simulator.regions.ProximityRoutingLeafBroker;

/**
 * A factory for creating brokers that use the location-based routing strategy.
 */
public class LocationBrokerFactory implements BrokerFactory {
    @Override
    public BoundedBroker createBroker(String name) {
        return new ProximityRoutingBroker(name);
    }

    @Override
    public BoundedBroker createLeafBroker(String name, Location p1, Location p2) {
        return new ProximityRoutingLeafBroker(name, p1, p2);
    }

    @Override
    public BoundedBroker createLeafBroker(String name) {
        return new ProximityRoutingLeafBroker(name);
    }
}