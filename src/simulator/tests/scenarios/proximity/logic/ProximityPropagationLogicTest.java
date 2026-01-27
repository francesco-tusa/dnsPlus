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
 * Verifies the "Heavy version, closest" routing logic using the 
 * deterministic structure of SimpleFixedTopologyGenerator.
 * * Topology Used:
 * Root -> child2 -> [grandchild2, grandchild3]
 * * Scenarios:
 * 1. Part A (Local): Subscriber and Publisher on 'grandchild2'.
 * 2. Part B (Cross-Broker): Subscriber on 'grandchild2', Publisher on 'grandchild3'.
 */
public class ProximityPropagationLogicTest extends TestScenario {

    @Override
    public String getTestName() {
        return "Proximity Propagation (Closest Algorithm) - Fixed Topology";
    }

    @Override
    public boolean run(TopologyFixture fixture) {
        SimulationBroker root = fixture.getRoot();

        // 1. Navigate the Fixed Topology
        // We target 'child2' because we know it has two leaves (grandchild2, grandchild3)
        SimulationBroker commonParent = findChildByName(root, "child2");
        
        if (commonParent == null) {
            logger.severe("Test Failed: Structure mismatch. Could not find 'child2' in root.");
            return false;
        }

        SimulationBroker leafA = findChildByName(commonParent, "grandchild2");
        SimulationBroker leafB = findChildByName(commonParent, "grandchild3");

        if (leafA == null || leafB == null) {
            logger.severe("Test Failed: Structure mismatch. 'child2' must have 'grandchild2' and 'grandchild3'.");
            return false;
        }

        logger.info("Topology Mapped: Subscriber Leaf=" + leafA.getName() + 
                   ", Sibling Leaf=" + leafB.getName() + 
                   ", Common Parent=" + commonParent.getName());

        // 2. Setup: Subscriber at (0,0) attached to Leaf A (grandchild2)
        Location subLoc = new Location(0, 0, 0);
        SubscriberWithLocation sub = new SubscriberWithLocation("Sub-LogicTest", subLoc);
        
        // Manually attach subscriber to Leaf A
        leafA.addChild(sub);
        
        // Subscriber sends subscription
        // In Proximity, this propagates up to Root, establishing the gradient.
        sub.send(new SubscriptionWithLocation(subLoc));

        // --- PART A: LOCAL PROPAGATION (On Leaf A) ---
        logger.info("--- PART A: Local Propagation (Same Broker: " + leafA.getName() + ") ---");
        
        // P1: Far (Dist 10)
        PublicationWithLocation p1 = createPub("P1", new Location(10, 0, 0));
        // P2: Medium (Dist 5)
        PublicationWithLocation p2 = createPub("P2", new Location(5, 0, 0));
        // P3: Worse (Dist 8) -> Should be IGNORED
        PublicationWithLocation p3 = createPub("P3", new Location(8, 0, 0));
        // P4: Best (Dist 2)
        PublicationWithLocation p4 = createPub("P4", new Location(2, 0, 0));

        try {
            // A. Send P1 (First is always accepted)
            leafA.processPublication(p1);
            FunctionalTestUtils.assertReceived(sub, 1); 

            // B. Send P2 (Closer -> Accepted)
            leafA.processPublication(p2);
            FunctionalTestUtils.assertReceived(sub, 2);

            // C. Send P3 (Farther than P2 -> Dropped)
            leafA.processPublication(p3);
            FunctionalTestUtils.assertReceived(sub, 2); // Count should NOT increase

            // D. Send P4 (Closest -> Accepted)
            leafA.processPublication(p4);
            FunctionalTestUtils.assertReceived(sub, 3);

        } catch (AssertionError e) {
            logger.severe("FAILURE IN PART A: " + e.getMessage());
            return false;
        }

        // --- PART B: CROSS-BROKER PROPAGATION (From Leaf B via Parent) ---
        logger.info("--- PART B: Cross-Broker Propagation (From Sibling: " + leafB.getName() + ") ---");

        // Current state of Subscriber: Received 3 pubs, Best Distance is 2 (from P4).
        // The propagation path is: leafB -> child2 -> leafA -> Subscriber.
        // Filtering happens at 'child2' (Common Parent) and 'leafA'.

        // P_Remote_1: Very Far (Dist 100) -> Pruned at child2
        PublicationWithLocation pRemote1 = createPub("Remote1", new Location(100, 0, 0));
        
        // P_Remote_2: Super Close (Dist 1) -> Forwarded by child2 -> Forwarded by leafA
        PublicationWithLocation pRemote2 = createPub("Remote2", new Location(1, 0, 0));

        // P_Remote_3: Worse again (Dist 1.5) -> Pruned at child2 (because it knows Sub has seen Dist 1)
        PublicationWithLocation pRemote3 = createPub("Remote3", new Location(1.5f, 0, 0));

        try {
            // E. Inject P_Remote_1 at Sibling (Worse than P4)
            leafB.processPublication(pRemote1);
            FunctionalTestUtils.assertReceived(sub, 3); // Count remains 3
            logger.info(" > Remote Far publication pruned correctly.");

            // F. Inject P_Remote_2 at Sibling (Better than P4)
            leafB.processPublication(pRemote2);
            FunctionalTestUtils.assertReceived(sub, 4); // Count increases to 4
            logger.info(" > Remote Closer publication received correctly.");

            // G. Inject P_Remote_3 at Sibling (Worse than P_Remote_2)
            leafB.processPublication(pRemote3);
            FunctionalTestUtils.assertReceived(sub, 4); // Count remains 4
            logger.info(" > Remote Intermediate publication pruned correctly.");

        } catch (AssertionError e) {
            logger.severe("FAILURE IN PART B: " + e.getMessage());
            return false;
        }

        logger.info("SUCCESS: Proximity propagation logic (Local & Cross-Broker) verified.");
        return true;
    }

    // --- Helpers ---

    private PublicationWithLocation createPub(String id, Location loc) {
        PublicationWithLocation p = new PublicationWithLocation(loc);
        p.setSource(null); // Simulate external arrival
        return p;
    }

    /**
     * Helper to find a specific child broker by name.
     * Robust against list ordering changes in the generator.
     */
    private SimulationBroker findChildByName(SimulationBroker parent, String name) {
        if (parent == null) return null;
        for (TreeNode child : parent.getChildren()) {
            if (child instanceof SimulationBroker) {
                SimulationBroker broker = (SimulationBroker) child;
                if (broker.getName().equals(name)) {
                    return broker;
                }
            }
        }
        return null;
    }
}