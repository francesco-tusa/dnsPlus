package simulator.topology.fixed;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.entities.SimulationBroker;
import simulator.regions.BoundedBroker;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.topology.factories.BrokerFactory;
import utils.CustomLogger;

/**
 * A generalized topology generator that builds a hardcoded test topology 
 * (Root -> 3 Children -> 4 Grandchildren).
 * * Refactored to support generic SimulationBrokers. It can build:
 * 1. A Region-based topology (if passed a BoundedBrokerFactory).
 * 2. A Coordinate-based topology (if passed a generic BrokerFactory).
 */
public class SimpleFixedTopologyGenerator extends AbstractTopologyFactory<SimpleFixedTopologyConfiguration, SimulationBroker> {

    private static final Logger logger = CustomLogger.getLogger(SimpleFixedTopologyGenerator.class.getName());
    
    // REFACTOR: Use the generic BrokerFactory interface, not BoundedBrokerFactory
    private final BrokerFactory brokerFactory;

    public SimpleFixedTopologyGenerator(BrokerFactory brokerFactory) {
        Objects.requireNonNull(brokerFactory, "BrokerFactory cannot be null.");
        this.brokerFactory = brokerFactory;
    }

    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        logger.fine("Initialising SimpleFixedTopologyGenerator...");
        if (!(genericConfig instanceof SimpleFixedTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of SimpleFixedTopologyConfiguration.");
        }
        this.config = (SimpleFixedTopologyConfiguration) genericConfig;
    }

    @Override
    protected SimulationBroker buildCoreTopology() {
        logger.fine("Building simple fixed topology...");
        
        // REFACTOR: Create generic SimulationBrokers
        SimulationBroker root = brokerFactory.createBroker("root");
        SimulationBroker child1 = brokerFactory.createBroker("child1");
        SimulationBroker child2 = brokerFactory.createBroker("child2");
        SimulationBroker child3 = brokerFactory.createBroker("child3");
        
        SimulationBroker grandchild1 = brokerFactory.createLeafBroker("grandchild1");
        SimulationBroker grandchild2 = brokerFactory.createLeafBroker("grandchild2");
        SimulationBroker grandchild3 = brokerFactory.createLeafBroker("grandchild3");
        SimulationBroker grandchild4 = brokerFactory.createLeafBroker("grandchild4");

        // Structure is valid for any implementation of SimulationBroker
        root.addChild(child1);
        root.addChild(child2);
        root.addChild(child3);
        child1.addChild(grandchild1);
        child2.addChild(grandchild2);
        child2.addChild(grandchild3);
        child3.addChild(grandchild4);
        
        logger.fine("Core broker topology built.");
        return root;
    }

    @Override
    public void attachSubscribers(SimulationBroker root) {
        logger.fine("Attaching fixed subscribers...");
        
        // REFACTOR: Look up nodes as generic SimulationBrokers
        SimulationBroker grandchild1 = TopologyAnalyser.findNodeByName(root, "grandchild1", SimulationBroker.class);
        SimulationBroker grandchild2 = TopologyAnalyser.findNodeByName(root, "grandchild2", SimulationBroker.class);
        SimulationBroker grandchild3 = TopologyAnalyser.findNodeByName(root, "grandchild3", SimulationBroker.class);
        SimulationBroker grandchild4 = TopologyAnalyser.findNodeByName(root, "grandchild4", SimulationBroker.class);

        // Attach clients using the helper that handles optional regions
        attachSubscriberHelper(grandchild1, "sub1", new Location(0, 0, 0));
        attachSubscriberHelper(grandchild2, "sub2", new Location(5, 2, 0));
        attachSubscriberHelper(grandchild3, "sub3", new Location(10, 1, 0));
        attachSubscriberHelper(grandchild4, "sub4", new Location(15, 1, 0));
        attachSubscriberHelper(grandchild1, "sub5", new Location(4, 3, 0));
        attachSubscriberHelper(grandchild2, "sub6", new Location(9, 5, 0));
        attachSubscriberHelper(grandchild3, "sub7", new Location(13, 3, 0));
        attachSubscriberHelper(grandchild4, "sub8", new Location(20, 5, 0));
        
        calculateBrokerRegions(root);
        logger.fine("Subscriber attachment complete.");
    }

    private void attachSubscriberHelper(SimulationBroker broker, String name, Location loc) {
        if (broker == null) return;
        
        SubscriberWithLocation sub = new SubscriberWithLocation(name, loc);
        broker.addChild(sub);
        
        // REFACTOR: Safe Type Check. Only update regions if the broker supports them.
        if (broker instanceof BoundedBroker bounded) {
            bounded.updateRegion(sub);
        }
    }

    @Override
    public void attachPublishers(SimulationBroker root) {
        logger.fine("Attaching fixed publishers...");
        
        SimulationBroker grandchild1 = TopologyAnalyser.findNodeByName(root, "grandchild1", SimulationBroker.class);
        SimulationBroker grandchild4 = TopologyAnalyser.findNodeByName(root, "grandchild4", SimulationBroker.class);
        
        if (grandchild1 != null) {
            grandchild1.addChild(new PublisherWithLocation("pub1", new Location(1, 1, 0)));
        }
        if (grandchild4 != null) {
            grandchild4.addChild(new PublisherWithLocation("pub2", new Location(18, 4, 0)));
        }
        logger.fine("Publisher attachment complete.");
    }

    private void calculateBrokerRegions(SimulationBroker root) {
        // REFACTOR: Safe Type Check. Skip optimization if brokers are not region-aware.
        if (!(root instanceof BoundedBroker)) {
            logger.fine("Skipping region calculation (Brokers are not BoundedBrokers).");
            return;
        }

        logger.fine("Calculating parent broker regions...");
        List<BoundedBroker> leaves = TopologyAnalyser.findLeafBrokers((BoundedBroker) root);
        
        for (BoundedBroker leaf : leaves) {
            if (leaf.getParentBroker() != null) {
                leaf.getParentBroker().updateRegion(leaf);
            }
        }
    }
}