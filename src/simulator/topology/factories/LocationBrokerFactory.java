package simulator.topology.factories;

import simulator.core.Location;
import simulator.regions.BrokerWithRegion;
import simulator.regions.BrokerWithRegionProcessingLocation;
import simulator.regions.LeafBrokerWithRegionProcessingLocation;

/**
 * A factory for creating brokers that use the location-based routing strategy.
 */
public class LocationBrokerFactory implements BrokerFactory {
    @Override
    public BrokerWithRegion createBroker(String name) {
        return new BrokerWithRegionProcessingLocation(name);
    }

    @Override
    public BrokerWithRegion createLeafBroker(String name, Location p1, Location p2) {
        return new LeafBrokerWithRegionProcessingLocation(name, p1, p2);
    }

    @Override
    public BrokerWithRegion createLeafBroker(String name) {
        return new LeafBrokerWithRegionProcessingLocation(name);
    }
}