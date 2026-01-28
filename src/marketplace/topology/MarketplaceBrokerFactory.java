package marketplace.topology;

import simulator.core.Location;
import simulator.regions.BoundedBroker;
import simulator.regions.ProximityRoutingLeafBroker;
import simulator.topology.factories.BoundedBrokerFactory;
import marketplace.agents.HierarchicalOrchestrationBroker;

/**
 * A factory for creating brokers that use the Hierarchical Orchestration
 * strategy.
 * Used for Marketplace Simulations.
 */
public class MarketplaceBrokerFactory implements BoundedBrokerFactory {
    @Override
    public BoundedBroker createBroker(String name) {
        return new HierarchicalOrchestrationBroker(name);
    }

    @Override
    public BoundedBroker createLeafBroker(String name, Location p1, Location p2) {
        // We can reuse the standard leaf broker, or create a marketplace specific one
        // if needed.
        // For now, we reuse, but typically we might want to wrap it or use
        // HierarchicalOrchestrationBroker as leaf too?
        // Actually, HierarchicalOrchestrationBroker extends ProximityRoutingBroker
        // which extends BoundedBroker.
        // But ProximityRoutingLeafBroker extends ProximityRoutingBroker too.
        // It's safer to use HierarchicalOrchestrationBroker as leaf if we want the
        // logic there too,
        // but leaf brokers usually just hold subs.

        // However, HierarchicalOrchestrationBroker IS a ProximityRoutingBroker.
        // Let's assume for now we just use the orchestration broker for all levels if
        // possible,
        // or if we need specific leaf logic (ProximityRoutingLeafBroker has strict
        // capacity logic),
        // we might need to subclass it.

        // Given the simulation code used `new HierarchicalOrchestrationBroker` for
        // leaves too,
        // let's return that.
        return new HierarchicalOrchestrationBroker(name, p1, p2);
    }

    @Override
    public BoundedBroker createLeafBroker(String name) {
        return new HierarchicalOrchestrationBroker(name);
    }
}
