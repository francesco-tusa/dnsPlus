package simulator.topology.fixed;

import simulator.Location;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.factories.BrokerFactory;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

/**
 * Generates a specific, hardcoded topology defined manually.
 * Now uses a BrokerFactory to remain agnostic of the broker implementation.
 */
public class FixedTestTopologyGenerator extends AbstractTopologyFactory<FixedTestTopologyConfiguration, BrokerWithRegion> {

    private final BrokerFactory brokerFactory;

    public FixedTestTopologyGenerator(BrokerFactory brokerFactory) {
        Objects.requireNonNull(brokerFactory, "BrokerFactory cannot be null.");
        this.brokerFactory = brokerFactory;
    }

    @Override
    protected void initialise(TopologyConfiguration genericConfig) {
        System.out.println("Initialising FixedTopologyGenerator...");
        if (!(genericConfig instanceof FixedTestTopologyConfiguration)) {
            throw new IllegalArgumentException("Configuration must be an instance of FixedTestTopologyConfiguration.");
        }
        this.config = (FixedTestTopologyConfiguration) genericConfig;
    }

    @Override
    protected BrokerWithRegion buildCoreTopology() {
        System.out.println("Building fixed core broker topology...");
        BrokerWithRegion root = brokerFactory.createBroker("root");
        BrokerWithRegion child1 = brokerFactory.createBroker("child1");
        BrokerWithRegion child2 = brokerFactory.createBroker("child2");
        BrokerWithRegion child3 = brokerFactory.createBroker("child3");
        BrokerWithRegion grandchild1 = brokerFactory.createLeafBroker("grandchild1", new Location(0,0,0), new Location(10,10,0));
        BrokerWithRegion grandchild2 = brokerFactory.createLeafBroker("grandchild2", new Location(0,0,0), new Location(10,10,0));
        BrokerWithRegion grandchild3 = brokerFactory.createLeafBroker("grandchild3", new Location(0,0,0), new Location(10,10,0));
        BrokerWithRegion grandchild4 = brokerFactory.createLeafBroker("grandchild4", new Location(0,0,0), new Location(10,10,0));

        root.addChild(child1);
        root.addChild(child2);
        root.addChild(child3);
        child1.addChild(grandchild1);
        child2.addChild(grandchild2);
        child2.addChild(grandchild3);
        child3.addChild(grandchild4);
        
        System.out.println("Core broker topology built.");
        return root;
    }

    @Override
    protected void attachSubscribers(BrokerWithRegion root) {
        System.out.println("Attaching fixed subscribers...");
        BrokerWithRegion grandchild1 = findNodeByName(root, "grandchild1");
        BrokerWithRegion grandchild2 = findNodeByName(root, "grandchild2");
        BrokerWithRegion grandchild3 = findNodeByName(root, "grandchild3");
        BrokerWithRegion grandchild4 = findNodeByName(root, "grandchild4");

        grandchild1.addChild(new SubscriberWithLocation("sub1", new Location(0, 0, 0)));
        grandchild2.addChild(new SubscriberWithLocation("sub2", new Location(5, 2, 0)));
        grandchild3.addChild(new SubscriberWithLocation("sub3", new Location(10, 1, 0)));
        grandchild4.addChild(new SubscriberWithLocation("sub4", new Location(15, 1, 0)));
        grandchild1.addChild(new SubscriberWithLocation("sub5", new Location(4, 3, 0)));
        grandchild2.addChild(new SubscriberWithLocation("sub6", new Location(9, 5, 0)));
        grandchild3.addChild(new SubscriberWithLocation("sub7", new Location(13, 3, 0)));
        grandchild4.addChild(new SubscriberWithLocation("sub8", new Location(20, 5, 0)));
        System.out.println("Subscriber attachment complete.");
    }

    @Override
    protected void attachPublishers(BrokerWithRegion root) {
        System.out.println("Attaching fixed publishers...");
        BrokerWithRegion grandchild1 = findNodeByName(root, "grandchild1");
        BrokerWithRegion grandchild4 = findNodeByName(root, "grandchild4");
        
        grandchild1.addChild(new PublisherWithLocation("pub1", new Location(7, 7, 0)));
        grandchild4.addChild(new PublisherWithLocation("pub2", new Location(18, 4, 0)));
        System.out.println("Publisher attachment complete.");
    }

    private BrokerWithRegion findNodeByName(TreeNode root, String name) {
        if (root == null || name == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (name.equals(current.getName()) && current instanceof BrokerWithRegion) {
                return (BrokerWithRegion) current;
            }
            if (current.getChildren() != null) {
                queue.addAll(current.getChildren());
            }
        }
        return null;
    }
}