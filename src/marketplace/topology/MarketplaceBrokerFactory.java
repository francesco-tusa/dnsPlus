package marketplace.topology;

import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.topology.factories.BoundedBrokerFactory;

/**
 * A factory for creating brokers that use the Hierarchical Orchestration
 * strategy.
 * Used for Marketplace Simulations.
 */
public class MarketplaceBrokerFactory implements BoundedBrokerFactory {
    @Override
    public BoundedBroker createBroker(String name) {
        return new marketplace.agents.MarketplaceBroker(name);
    }

    @Override
    public BoundedBroker createLeafBroker(String name, Location p1, Location p2) {
        return new marketplace.agents.MarketplaceBroker(name, p1, p2);
    }

    @Override
    public BoundedBroker createLeafBroker(String name) {
        return new marketplace.agents.MarketplaceBroker(name);
    }
}
