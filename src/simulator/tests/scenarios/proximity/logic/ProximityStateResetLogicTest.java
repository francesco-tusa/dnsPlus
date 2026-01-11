package simulator.tests.scenarios.proximity.logic;

import java.util.logging.Logger;

import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.entities.SimulationBroker;
import simulator.events.PublicationWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import utils.CustomLogger;

/**
 * Comprehensive test for Proximity Routing (Heavy/Closest).
 * * COVERS:
 * 1. Differential Forwarding: Ensures that two subscribers connected to the SAME broker 
 * receive different streams based on their unique locations.
 * 2. State Reset: Verifies that re-subscribing resets the "Best Distance" filter, 
 * allowing "worse" publications to be received again (simulating context switch).
 */
public class ProximityStateResetLogicTest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(ProximityStateResetLogicTest.class.getName());

    @Override
    public String getTestName() {
        return "Proximity Network Logic (Isolation & Reset)";
    }

    @Override
    public boolean run(TopologyFixture fixture) {
        SimulationBroker root = fixture.getRoot();
        SimulationBroker leaf = findLeaf(root);

        if (leaf == null) {
            logger.severe("Test Failed: No leaf broker found.");
            return false;
        }

        // --- SETUP: Create two Subscribers at different locations ---
        // Sub A is at (0,0)
        Location locA = new Location(0, 0, 0);
        SubscriberWithLocation subA = new SubscriberWithLocation("SubA", locA);
        
        // Sub B is at (20,0)
        Location locB = new Location(20, 0, 0);
        SubscriberWithLocation subB = new SubscriberWithLocation("SubB", locB);

        leaf.addChild(subA);
        leaf.addChild(subB);

        // Both subscribe
        subA.send(new SubscriptionWithLocation(locA));
        subB.send(new SubscriptionWithLocation(locB));

        logger.info("Setup: SubA at (0,0), SubB at (20,0) attached to " + leaf.getName());

        // --- PHASE 1: Differential Forwarding ---
        // We will send publications to the Leaf (simulating arrival from Parent or other source)
        
        // Pub 1: At (10,0). Dist to A=10, Dist to B=10.
        // Expectation: Both receive (First interaction).
        PublicationWithLocation pub1 = createPub(new Location(10, 0, 0));
        leaf.processPublication(pub1);
        
        if (!check(subA, 1, "Phase 1-1 (Init)")) return false;
        if (!check(subB, 1, "Phase 1-1 (Init)")) return false;

        // Pub 2: At (2,0). Dist to A=2 (Better), Dist to B=18 (Worse).
        // Expectation: Sub A receives. Sub B ignores (18 > 10).
        PublicationWithLocation pub2 = createPub(new Location(2, 0, 0));
        leaf.processPublication(pub2);

        if (!check(subA, 2, "Phase 1-2 (Close to A)")) return false;
        if (!check(subB, 1, "Phase 1-2 (Far from B)")) return false; // Should NOT increment

        // Pub 3: At (18,0). Dist to A=18 (Worse than 2), Dist to B=2 (Better than 10).
        // Expectation: Sub A ignores. Sub B receives.
        PublicationWithLocation pub3 = createPub(new Location(18, 0, 0));
        leaf.processPublication(pub3);

        if (!check(subA, 2, "Phase 1-3 (Far from A)")) return false; // Should NOT increment
        if (!check(subB, 2, "Phase 1-3 (Close to B)")) return false;

        logger.info("SUCCESS: Phase 1 (Isolation) Passed. Subscribers filtered correctly.");

        // --- PHASE 2: State Reset (Re-subscription) ---
        // Sub A currently holds "Best Distance = 2".
        // If we send a pub at distance 5, it will be dropped.
        // BUT, if Sub A re-subscribes, it should reset its filter.

        // 1. Send "Worse" pub to verify drop
        PublicationWithLocation pubWorse = createPub(new Location(5, 0, 0));
        leaf.processPublication(pubWorse);
        if (!check(subA, 2, "Phase 2-1 (Pre-Reset Drop)")) return false;

        // 2. Re-Subscribe
        logger.info("Triggering Re-subscription for SubA...");
        subA.send(new SubscriptionWithLocation(locA));

        // 3. Send the SAME "Worse" pub again
        // Expectation: Received! (Because filter was reset to MAX_VALUE)
        leaf.processPublication(pubWorse);
        if (!check(subA, 3, "Phase 2-2 (Post-Reset Receive)")) return false;

        logger.info("SUCCESS: Phase 2 (State Reset) Passed.");
        return true;
    }

    private boolean check(SubscriberWithLocation sub, int expected, String step) {
        int actual = sub.getnPublications();
        if (actual != expected) {
            logger.severe("FAILURE [" + step + "]: " + sub.getName() + 
                          " expected " + expected + ", got " + actual);
            return false;
        }
        return true;
    }

    private PublicationWithLocation createPub(Location loc) {
        PublicationWithLocation p = new PublicationWithLocation(loc);
        p.setSource(null);
        return p;
    }

    private SimulationBroker findLeaf(SimulationBroker broker) {
        if (broker.getChildren().isEmpty()) return broker;
        for (var child : broker.getChildren()) {
            if (child instanceof SimulationBroker) return findLeaf((SimulationBroker) child);
        }
        return broker;
    }
}