package simulator.topology.fixed;

import java.util.Objects;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.entities.SimulationBroker;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.analysis.TopologyAnalyser;
import simulator.topology.factories.BrokerFactory;
import utils.CustomLogger;

/**
 * Abstract generator that defines the FIXED structural layout of the topology
 * (Root -> 3 Children -> 4 Grandchildren) but delegates node maintenance to subclasses.
 * * @param <C> The specific configuration type.
 */
public abstract class AbstractFixedTopologyGenerator<C extends SimpleFixedTopologyConfiguration> 
        extends AbstractTopologyFactory<C, SimulationBroker> {

    protected static final Logger logger = CustomLogger.getLogger(AbstractFixedTopologyGenerator.class.getName());
    protected final BrokerFactory brokerFactory;

    public AbstractFixedTopologyGenerator(BrokerFactory brokerFactory) {
        Objects.requireNonNull(brokerFactory, "BrokerFactory cannot be null.");
        this.brokerFactory = brokerFactory;
    }

    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        // Unchecked cast is safe because the factory enforces C in generic bounds
        this.config = (C) genericConfig;
    }

    @Override
    protected SimulationBroker buildCoreTopology() {
        logger.fine("Building core fixed topology structure...");
        
        SimulationBroker root = brokerFactory.createBroker("root");
        SimulationBroker child1 = brokerFactory.createBroker("child1");
        SimulationBroker child2 = brokerFactory.createBroker("child2");
        SimulationBroker child3 = brokerFactory.createBroker("child3");
        
        SimulationBroker grandchild1 = brokerFactory.createLeafBroker("grandchild1");
        SimulationBroker grandchild2 = brokerFactory.createLeafBroker("grandchild2");
        SimulationBroker grandchild3 = brokerFactory.createLeafBroker("grandchild3");
        SimulationBroker grandchild4 = brokerFactory.createLeafBroker("grandchild4");

        root.addChild(child1);
        root.addChild(child2);
        root.addChild(child3);
        child1.addChild(grandchild1);
        child2.addChild(grandchild2);
        child2.addChild(grandchild3);
        child3.addChild(grandchild4);
        
        return root;
    }

    @Override
    public void attachSubscribers(SimulationBroker root) {
        logger.fine("Attaching fixed subscribers...");
        
        SimulationBroker grandchild1 = TopologyAnalyser.findNodeByName(root, "grandchild1", SimulationBroker.class);
        SimulationBroker grandchild2 = TopologyAnalyser.findNodeByName(root, "grandchild2", SimulationBroker.class);
        SimulationBroker grandchild3 = TopologyAnalyser.findNodeByName(root, "grandchild3", SimulationBroker.class);
        SimulationBroker grandchild4 = TopologyAnalyser.findNodeByName(root, "grandchild4", SimulationBroker.class);

        attachSubscriberHelper(grandchild1, "sub1", new Location(0, 0, 0));
        attachSubscriberHelper(grandchild2, "sub2", new Location(5, 2, 0));
        attachSubscriberHelper(grandchild3, "sub3", new Location(10, 1, 0));
        attachSubscriberHelper(grandchild4, "sub4", new Location(15, 1, 0));
        attachSubscriberHelper(grandchild1, "sub5", new Location(4, 3, 0));
        attachSubscriberHelper(grandchild2, "sub6", new Location(9, 5, 0));
        attachSubscriberHelper(grandchild3, "sub7", new Location(13, 3, 0));
        attachSubscriberHelper(grandchild4, "sub8", new Location(20, 5, 0));
        
        finalizeTopology(root);
        logger.fine("Subscriber attachment complete.");
    }

    private void attachSubscriberHelper(SimulationBroker broker, String name, Location loc) {
        if (broker == null) return;
        SubscriberWithLocation sub = new SubscriberWithLocation(name, loc);
        broker.addChild(sub);
        postAttachSubscriber(broker, sub);
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
    }

    /**
     * Hook to perform specific logic after a subscriber is attached (e.g. update regions).
     */
    protected abstract void postAttachSubscriber(SimulationBroker broker, SubscriberWithLocation sub);

    /**
     * Hook to perform final topology calculations (e.g. bubble up regions).
     */
    protected abstract void finalizeTopology(SimulationBroker root);
}