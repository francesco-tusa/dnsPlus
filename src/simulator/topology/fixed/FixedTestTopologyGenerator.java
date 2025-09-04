package simulator.topology.fixed;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.topology.AbstractTopologyFactory;
import simulator.topology.TopologyConfiguration;
import simulator.topology.factories.BrokerFactory;
import utils.CustomLogger;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Generates a specific, hardcoded topology defined manually.
 * Now uses a BrokerFactory to remain agnostic of the broker implementation and
 * correctly calculates parent regions after subscribers are attached.
 */
public class FixedTestTopologyGenerator extends AbstractTopologyFactory<FixedTestTopologyConfiguration, BrokerWithRegion> {

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
    protected BrokerWithRegion buildCoreTopology() {
        logger.fine("Building fixed core broker topology...");
        BrokerWithRegion root = brokerFactory.createBroker("root");
        BrokerWithRegion child1 = brokerFactory.createBroker("child1");
        BrokerWithRegion child2 = brokerFactory.createBroker("child2");
        BrokerWithRegion child3 = brokerFactory.createBroker("child3");
        
        BrokerWithRegion grandchild1 = brokerFactory.createLeafBroker("grandchild1");
        BrokerWithRegion grandchild2 = brokerFactory.createLeafBroker("grandchild2");
        BrokerWithRegion grandchild3 = brokerFactory.createLeafBroker("grandchild3");
        BrokerWithRegion grandchild4 = brokerFactory.createLeafBroker("grandchild4");

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
    protected void attachSubscribers(BrokerWithRegion root) {
        logger.fine("Attaching fixed subscribers...");
        BrokerWithRegion grandchild1 = findNodeByName(root, "grandchild1");
        BrokerWithRegion grandchild2 = findNodeByName(root, "grandchild2");
        BrokerWithRegion grandchild3 = findNodeByName(root, "grandchild3");
        BrokerWithRegion grandchild4 = findNodeByName(root, "grandchild4");

        // Attach subscribers and immediately update the leaf broker's region
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
        
        // After leaf regions are defined by subscribers, calculate the parent regions.
        calculateBrokerRegions(root);
        
        logger.fine("Subscriber attachment complete.");
    }

    @Override
    protected void attachPublishers(BrokerWithRegion root) {
        logger.fine("Attaching fixed publishers...");
        BrokerWithRegion grandchild1 = findNodeByName(root, "grandchild1");
        BrokerWithRegion grandchild4 = findNodeByName(root, "grandchild4");
        
        grandchild1.addChild(new PublisherWithLocation("pub1", new Location(7, 7, 0)));
        grandchild4.addChild(new PublisherWithLocation("pub2", new Location(18, 4, 0)));
        logger.fine("Publisher attachment complete.");
    }

    /**
     * Manually triggers the region calculation for all parent brokers, starting from the leaves.
     * This ensures parent regions are calculated based on the final leaf regions.
     */
    private void calculateBrokerRegions(BrokerWithRegion root) {
        logger.fine("Calculating parent broker regions...");
        List<BrokerWithRegion> leaves = findLeafBrokers(root);
        for (BrokerWithRegion leaf : leaves) {
            if (leaf.getParentBroker() != null) {
                leaf.getParentBroker().updateRegion(leaf);
            }
        }
    }

    private List<BrokerWithRegion> findLeafBrokers(BrokerWithRegion root) {
        List<BrokerWithRegion> leaves = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        if (root != null) queue.add(root);
        
        while(!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof BrokerWithRegion) {
                boolean hasBrokerChild = false;
                for (TreeNode child : current.getChildren()) {
                    if (child instanceof BrokerWithRegion) {
                        hasBrokerChild = true;
                        break;
                    }
                }
                if (!hasBrokerChild) {
                    leaves.add((BrokerWithRegion) current);
                }
            }
            if (current.getChildren() != null) queue.addAll(current.getChildren());
        }
        return leaves;
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