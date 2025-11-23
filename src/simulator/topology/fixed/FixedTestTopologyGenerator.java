package simulator.topology.fixed;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BoundedBroker;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.factories.BrokerFactory;
import utils.CustomLogger;

public class FixedTestTopologyGenerator extends AbstractTopologyFactory<FixedTestTopologyConfiguration, BoundedBroker> {

    private static final Logger logger = CustomLogger.getLogger(FixedTestTopologyGenerator.class.getName());
    private final BrokerFactory brokerFactory;

    public FixedTestTopologyGenerator(BrokerFactory brokerFactory) {
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
        BoundedBroker grandchild1 = findNodeByName(root, "grandchild1", BoundedBroker.class);
        BoundedBroker grandchild2 = findNodeByName(root, "grandchild2", BoundedBroker.class);
        BoundedBroker grandchild3 = findNodeByName(root, "grandchild3", BoundedBroker.class);
        BoundedBroker grandchild4 = findNodeByName(root, "grandchild4", BoundedBroker.class);

        // After adding a subscriber, we must explicitly update the leaf broker's region.
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
        BoundedBroker grandchild1 = findNodeByName(root, "grandchild1", BoundedBroker.class);
        BoundedBroker grandchild4 = findNodeByName(root, "grandchild4", BoundedBroker.class);
        
        grandchild1.addChild(new PublisherWithLocation("pub1", new Location(1, 1, 0)));
        grandchild4.addChild(new PublisherWithLocation("pub2", new Location(18, 4, 0)));
        logger.fine("Publisher attachment complete.");
    }

    private void calculateBrokerRegions(BoundedBroker root) {
        logger.fine("Calculating parent broker regions...");
        List<BoundedBroker> leaves = findLeafBrokers(root);
        for (BoundedBroker leaf : leaves) {
            if (leaf.getParentBroker() != null) {
                leaf.getParentBroker().updateRegion(leaf);
            }
        }
    }
    
    private List<BoundedBroker> findLeafBrokers(BoundedBroker root) {
        List<BoundedBroker> leaves = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);
        
        while(!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof BoundedBroker) {
                boolean hasBrokerChild = false;
                for (TreeNode child : current.getChildren()) {
                    if (child instanceof BoundedBroker) {
                        hasBrokerChild = true;
                        break;
                    }
                }
                if (!hasBrokerChild) {
                    leaves.add((BoundedBroker) current);
                }
            }
            if (current.getChildren() != null) queue.addAll(current.getChildren());
        }
        return leaves;
    }

    private <T extends TreeNode> T findNodeByName(TreeNode root, String name, Class<T> type) {
        if (root == null || name == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (type.isInstance(current) && name.equals(current.getName())) {
                return type.cast(current);
            }
            if (current.getChildren() != null) {
                queue.addAll(current.getChildren());
            }
        }
        return null;
    }
}