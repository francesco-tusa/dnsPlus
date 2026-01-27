package simulator.topology.fixed;

import simulator.entities.SubscriberWithLocation;
import simulator.entities.SimulationBroker;
import simulator.topology.factories.BrokerFactory;

/**
 * Concrete implementation for Coordinate-based topologies (No region updates).
 */
public class CoordinateFixedTopologyGenerator extends AbstractFixedTopologyGenerator<SimpleCoordinateFixedTopologyConfiguration> {

    public CoordinateFixedTopologyGenerator(BrokerFactory brokerFactory) {
        super(brokerFactory);
    }

    @Override
    protected void postAttachSubscriber(SimulationBroker broker, SubscriberWithLocation sub) {
        // No-op for coordinate routing (no regions to update)
    }

    @Override
    protected void finalizeTopology(SimulationBroker root) {
        // No-op for coordinate routing
    }
}