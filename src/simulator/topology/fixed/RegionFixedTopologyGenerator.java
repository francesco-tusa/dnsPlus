package simulator.topology.fixed;

import java.util.List;
import simulator.entities.SubscriberWithLocation;
import simulator.entities.SimulationBroker;
import simulator.regions.BoundedBroker;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.topology.factories.BoundedBrokerFactory;

/**
 * Concrete implementation for Region-based topologies (uses BoundedBrokers).
 */
public class RegionFixedTopologyGenerator extends AbstractFixedTopologyGenerator<SimpleRegionFixedTopologyConfiguration> {

    public RegionFixedTopologyGenerator(BoundedBrokerFactory brokerFactory) {
        super(brokerFactory);
    }

    @Override
    protected void postAttachSubscriber(SimulationBroker broker, SubscriberWithLocation sub) {
        if (broker instanceof BoundedBroker bounded) {
            bounded.updateRegion(sub);
        }
    }

    @Override
    protected void finalizeTopology(SimulationBroker root) {
        if (!(root instanceof BoundedBroker)) return;

        logger.fine("Calculating parent broker regions...");
        List<BoundedBroker> leaves = TopologyAnalyser.findLeafBrokers((BoundedBroker) root);
        
        for (BoundedBroker leaf : leaves) {
            if (leaf.getParentBroker() != null) {
                leaf.getParentBroker().updateRegion(leaf);
            }
        }
    }
}