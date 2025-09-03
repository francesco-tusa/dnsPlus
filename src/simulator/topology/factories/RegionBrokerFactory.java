package simulator.topology.factories;

import simulator.core.Location;
import simulator.regions.BrokerWithRegion;
import simulator.regions.BrokerWithRegionProcessingRegion;
import simulator.regions.LeafBrokerWithRegionProcessingRegion;

/**
 * A factory for creating brokers that use the region-based routing strategy.
 */
public class RegionBrokerFactory implements BrokerFactory {
    @Override
    public BrokerWithRegion createBroker(String name) {
        return new BrokerWithRegionProcessingRegion(name);
    }

    @Override
    public BrokerWithRegion createLeafBroker(String name, Location p1, Location p2) {
        return new LeafBrokerWithRegionProcessingRegion(name, p1, p2);
    }

    @Override
    public BrokerWithRegion createLeafBroker(String name) {
        return new LeafBrokerWithRegionProcessingRegion(name);
    }
}