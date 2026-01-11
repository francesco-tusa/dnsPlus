package simulator.tests.scenarios.proximity.logic;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.entities.SimulationBroker;
import simulator.events.PublicationWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.tests.framework.FunctionalTestUtils;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

/**
 * Verifies the "Heavy version, closest" routing logic.
 * * Logic Tested:
 * 1. A subscriber sends a subscription.
 * 2. Publisher sends Pub A (Far) -> Received (First is always best).
 * 3. Publisher sends Pub B (Closer) -> Received (Improvement).
 * 4. Publisher sends Pub C (Farther than B) -> Dropped (Pruning).
 * 5. Publisher sends Pub D (Closest) -> Received (Improvement).
 */
public class ProximityPropagationLogicTest extends TestScenario {

    @Override
    public String getTestName() {
        return "Proximity Propagation (Closest Algorithm)";
    }

    @Override
    public boolean run(TopologyFixture fixture) {
        SimulationBroker root = fixture.getRoot();
        SimulationBroker leaf = findLeaf(root);

        if (leaf == null) {
            logger.severe("Test Failed: No leaf broker found in topology.");
            return false;
        }

        // 1. Setup: Subscriber at (0,0)
        Location subLoc = new Location(0, 0, 0);
        SubscriberWithLocation sub = new SubscriberWithLocation("Sub1", subLoc);
        
        // Manually attach subscriber to the leaf for this test isolation
        leaf.addChild(sub);
        
        // Subscriber sends subscription
        sub.send(new SubscriptionWithLocation(subLoc));

        logger.info("Testing Proximity on Leaf: " + leaf.getName() + " with Subscriber at " + subLoc);

        // 2. Define Publications at varying distances
        // Note: Using constructor PublicationWithLocation(Location)
        
        // P1: Far (Dist 10)
        PublicationWithLocation p1 = createPub(new Location(10, 0, 0));
        // P2: Medium (Dist 5)
        PublicationWithLocation p2 = createPub(new Location(5, 0, 0));
        // P3: Worse (Dist 8) -> Should be IGNORED (Farther than Medium)
        PublicationWithLocation p3 = createPub(new Location(8, 0, 0));
        // P4: Best (Dist 2)
        PublicationWithLocation p4 = createPub(new Location(2, 0, 0));

        // 3. Execution & Assertions
        
        try {
            // A. Send P1 (First is always accepted)
            leaf.processPublication(p1);
            FunctionalTestUtils.assertReceived(sub, 1); // Helper from TestScenario

            // B. Send P2 (Closer -> Accepted)
            leaf.processPublication(p2);
            FunctionalTestUtils.assertReceived(sub, 2);

            // C. Send P3 (Farther than P2 -> Dropped)
            leaf.processPublication(p3);
            FunctionalTestUtils.assertReceived(sub, 2); // Count should NOT increase

            // D. Send P4 (Closest -> Accepted)
            leaf.processPublication(p4);
            FunctionalTestUtils.assertReceived(sub, 3);

        } catch (AssertionError e) {
            logger.severe("FAILURE: " + e.getMessage());
            return false;
        }

        logger.info("SUCCESS: Proximity propagation logic verified.");
        return true;
    }

    private PublicationWithLocation createPub(Location loc) {
        PublicationWithLocation p = new PublicationWithLocation(loc);
        p.setSource(null); // Simulate external arrival
        return p;
    }

    private SimulationBroker findLeaf(SimulationBroker broker) {
        if (broker.getChildren().isEmpty()) return broker;
        
        for (TreeNode child : broker.getChildren()) {
            if (child instanceof SimulationBroker) {
                return findLeaf((SimulationBroker) child);
            }
        }
        // If all children are subscribers (leaf node), return self
        return broker;
    }
}