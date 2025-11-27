package simulator.topology.fixed;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BoundedBroker;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.analysis.TopologyAnalyzer; // Import Analyzer
import simulator.topology.factories.BoundedBrokerFactory;
import utils.CustomLogger;

public class FixedTestTopologyGenerator extends AbstractTopologyFactory<FixedTestTopologyConfiguration, BoundedBroker> {

    private static final Logger logger = CustomLogger.getLogger(FixedTestTopologyGenerator.class.getName());
    private final BoundedBrokerFactory brokerFactory;

    public FixedTestTopologyGenerator(BoundedBrokerFactory brokerFactory) {
        Objects.requireNonNull(brokerFactory, "BrokerFactory cannot be null.");
        this.brokerFactory = brokerFactory;
    }

    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        logger.fine("Initialising FixedTopologyGenerator...");
        if (!(genericConfig instanceof FixedTestTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of FixedTestTopologyConfiguration.");
        }
        this.config = (FixedTestTopologyConfiguration) genericConfig;
    }

    @Override
    protected BoundedBroker buildCoreTopology() {
        logger.fine("Building fixed core broker topology...");
        BoundedBroker root = brokerFactory.createBroker("root");
        BoundedBroker child1 = brokerFactory.createBroker("child1");
        BoundedBroker child2 = brokerFactory.createBroker("child2");
        BoundedBroker child3 = brokerFactory.createBroker("child3");
        
        BoundedBroker grandchild1 = brokerFactory.createLeafBroker("grandchild1");
        BoundedBroker grandchild2 = brokerFactory.createLeafBroker("grandchild2");
        BoundedBroker grandchild3 = brokerFactory.createLeafBroker("grandchild3");
        BoundedBroker grandchild4 = brokerFactory.createLeafBroker("grandchild4");

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
    public void attachSubscribers(BoundedBroker root) {
        logger.fine("Attaching fixed subscribers...");
        
        // Use TopologyAnalyzer instead of private method
        BoundedBroker grandchild1 = TopologyAnalyzer.findNodeByName(root, "grandchild1", BoundedBroker.class);
        BoundedBroker grandchild2 = TopologyAnalyzer.findNodeByName(root, "grandchild2", BoundedBroker.class);
        BoundedBroker grandchild3 = TopologyAnalyzer.findNodeByName(root, "grandchild3", BoundedBroker.class);
        BoundedBroker grandchild4 = TopologyAnalyzer.findNodeByName(root, "grandchild4", BoundedBroker.class);

        SubscriberWithLocation sub1 = new SubscriberWithLocation("sub1", new Location(0, 0, 0));
        grandchild1.addChild(sub1);
        grandchild1.updateRegion(sub1);

        SubscriberWithLocation sub2 = new SubscriberWithLocation("sub2", new Location(5, 2, 0));
        grandchild2.addChild(sub2);
        grandchild2.updateRegion(sub2);

        SubscriberWithLocation sub3 = new SubscriberWithLocation("sub3", new Location(10, 1, 0));
        grandchild3.addChild(sub3);
        grandchild3.updateRegion(sub3);
        
        SubscriberWithLocation sub4 = new SubscriberWithLocation("sub4", new Location(15, 1, 0));
        grandchild4.addChild(sub4);
        grandchild4.updateRegion(sub4);

        SubscriberWithLocation sub5 = new SubscriberWithLocation("sub5", new Location(4, 3, 0));
        grandchild1.addChild(sub5);
        grandchild1.updateRegion(sub5);

        SubscriberWithLocation sub6 = new SubscriberWithLocation("sub6", new Location(9, 5, 0));
        grandchild2.addChild(sub6);
        grandchild2.updateRegion(sub6);

        SubscriberWithLocation sub7 = new SubscriberWithLocation("sub7", new Location(13, 3, 0));
        grandchild3.addChild(sub7);
        grandchild3.updateRegion(sub7);

        SubscriberWithLocation sub8 = new SubscriberWithLocation("sub8", new Location(20, 5, 0));
        grandchild4.addChild(sub8);
        grandchild4.updateRegion(sub8);
        
        calculateBrokerRegions(root);
        logger.fine("Subscriber attachment complete.");
    }

    @Override
    public void attachPublishers(BoundedBroker root) {
        logger.fine("Attaching fixed publishers...");
        // Use TopologyAnalyzer
        BoundedBroker grandchild1 = TopologyAnalyzer.findNodeByName(root, "grandchild1", BoundedBroker.class);
        BoundedBroker grandchild4 = TopologyAnalyzer.findNodeByName(root, "grandchild4", BoundedBroker.class);
        
        grandchild1.addChild(new PublisherWithLocation("pub1", new Location(1, 1, 0)));
        grandchild4.addChild(new PublisherWithLocation("pub2", new Location(18, 4, 0)));
        logger.fine("Publisher attachment complete.");
    }

    private void calculateBrokerRegions(BoundedBroker root) {
        logger.fine("Calculating parent broker regions...");
        // Use TopologyAnalyzer
        List<BoundedBroker> leaves = TopologyAnalyzer.findLeafBrokers(root);
        
        for (BoundedBroker leaf : leaves) {
            if (leaf.getParentBroker() != null) {
                leaf.getParentBroker().updateRegion(leaf);
            }
        }
    }
}