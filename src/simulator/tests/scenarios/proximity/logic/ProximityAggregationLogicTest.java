package simulator.tests.scenarios.proximity.logic;

import java.util.List;
import simulator.core.TreeNode;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

/**
 * Verifies that ProximityRoutingBroker aggregates multiple downstream subscriptions
 * into a single upstream subscription.
 */
public class ProximityAggregationLogicTest extends TestScenario {

    @Override
    public String getTestName() {
        return "Subscription Aggregation (Proximity)";
    }

    @Override
    public boolean run(TopologyFixture fixture) {
        // 1. Identify a Leaf Broker and its Parent
        SimulationBroker leaf = findLeafWithSubscribers(fixture.getRoot());
        if (leaf == null) {
            logger.warning("Skipping: Could not find a leaf broker with subscribers.");
            return true; // Skip if topology doesn't support it
        }

        SimulationBroker parent = (SimulationBroker) leaf.getParentBroker();
        if (parent == null) {
            logger.warning("Skipping: Leaf has no parent.");
            return true;
        }

        logger.info("Testing Aggregation on Leaf: " + leaf.getName() + " -> Parent: " + parent.getName());

        // 2. Identify two subscribers on this leaf
        List<SubscriberWithLocation> subscribers = getSubscribers(leaf);
        if (subscribers.size() < 2) {
            logger.warning("Skipping: Leaf " + leaf.getName() + " needs at least 2 subscribers for this test.");
            return true;
        }

        SubscriberWithLocation sub1 = subscribers.get(0);
        SubscriberWithLocation sub2 = subscribers.get(1);

        // 3. First Subscription
        logger.info("Step 1: " + sub1.getName() + " subscribing...");
        sub1.send(new SubscriptionWithLocation(sub1.getLocation()));

        // Assert: Parent should have received 1 subscription from Leaf
        if (!assertInputCount(parent, leaf, 1)) return false;

        // 4. Second Subscription (Should be Absorbed)
        logger.info("Step 2: " + sub2.getName() + " subscribing...");
        sub2.send(new SubscriptionWithLocation(sub2.getLocation()));

        // Assert: Parent should STILL have only 1 subscription from Leaf
        if (!assertInputCount(parent, leaf, 1)) return false;

        logger.info("SUCCESS: Subscription aggregated correctly.");
        return true;
    }

    private SimulationBroker findLeafWithSubscribers(TreeNode node) {
        if (node.getChildren() == null) return null;
        
        // Check if I am a leaf with subscribers
        if (node instanceof SimulationBroker broker) {
            List<SubscriberWithLocation> subs = getSubscribers(broker);
            if (subs.size() >= 2) return broker;
        }

        // Recurse
        for (TreeNode child : node.getChildren()) {
            SimulationBroker result = findLeafWithSubscribers(child);
            if (result != null) return result;
        }
        return null;
    }

    private List<SubscriberWithLocation> getSubscribers(SimulationBroker broker) {
        return broker.getChildren().stream()
                .filter(c -> c instanceof SubscriberWithLocation)
                .map(c -> (SubscriberWithLocation) c)
                .toList();
    }

    private boolean assertInputCount(SimulationBroker parent, SimulationBroker source, int expected) {
        List<SimulationSubscription> subs = parent.getInputSubscriptions().get(source);
        int actual = (subs == null) ? 0 : subs.size();
        
        if (actual != expected) {
            logger.severe("FAILURE: Parent " + parent.getName() + " has " + actual + 
                          " subscriptions from " + source.getName() + " (Expected " + expected + ")");
            return false;
        }
        return true;
    }
}