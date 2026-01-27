package simulator.topology.factories;

import simulator.core.Location;
import simulator.regions.BoundedBroker;

public interface BoundedBrokerFactory extends BrokerFactory {
    @Override
    BoundedBroker createBroker(String name);

    @Override
    BoundedBroker createLeafBroker(String name);

    BoundedBroker createLeafBroker(String name, Location p1, Location p2);
}