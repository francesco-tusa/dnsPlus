package simulator.tests.scenarios.proximity.logic;

import simulator.core.Location;
import simulator.entities.SimulationBroker;
import simulator.events.PublicationWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.ProximityRoutingLeafBroker;
import simulator.regions.policy.FixedCounterBrakeStrategy;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

/**
 * Verifies the "Fixed Brake Rule" works independently per quadrant.
 * * Logic Tested:
 * 1. Configures a Leaf Broker with a Fixed Brake Strategy (Limit=5 per quadrant).
 * 2. Iterates through all 4 quadrants (NE, NW, SW, SE).
 * 3. For each quadrant, sends a burst of 10 publications.
 * 4. Asserts that exactly 5 pass for THAT quadrant (proving independence).
 * 5. Asserts that subsequent traffic for that quadrant is permanently blocked.
 */
public class ProximityBrakeLogicTest extends TestScenario {

    private static final int BRAKE_LIMIT = 5;
    private static final int BURST_SIZE = 10;

    @Override
    public String getTestName() {
        return "Proximity Brake Logic Test (Multi-Quadrant Independence)";
    }

    @Override
    public boolean run(TopologyFixture fixture) {
        // 1. Setup Topology
        SimulationBroker root = fixture.getRoot();
        SimulationBroker leaf = findFirstLeaf(root);

        if (leaf == null || leaf.getParentBroker() == null) {
            logger.severe("Skipping Test: Topology must have at least one Leaf with a Parent.");
            return false;
        }

        if (!(leaf instanceof ProximityRoutingLeafBroker)) {
            logger.severe("Skipping Test: Leaf broker is not a ProximityRoutingLeafBroker.");
            return false;
        }

        ProximityRoutingLeafBroker proxLeaf = (ProximityRoutingLeafBroker) leaf;
        SimulationBroker parent = leaf.getParentBroker();

        logger.info("Testing Per-Quadrant Brake on Broker: " + proxLeaf.getName());

        // 2. Inject Brake Strategy
        FixedCounterBrakeStrategy strategy = new FixedCounterBrakeStrategy(BRAKE_LIMIT);
        proxLeaf.setBrakeStrategy(strategy);

        // 3. Setup Subscription (Center) so all pubs match interest
        SubscriptionWithLocation sub = new SubscriptionWithLocation(proxLeaf.getRegion().getCenter());
        sub.setSource(parent);
        proxLeaf.addSubscription(sub);

        // 4. Define Test Points for all 4 Quadrants
        // We use a small delta (0.1) to ensure we stay inside the region (assuming reasonable region size)
        // while clearly landing in different quadrants relative to the center.
        Location center = proxLeaf.getRegion().getCenter();
        double cx = center.getX();
        double cy = center.getY();

        // Array of test configurations: {Name, OffsetX, OffsetY}
        // Q0: NE (+,+), Q1: NW (-,+), Q2: SW (-,-), Q3: SE (+,-)
        Object[][] testQuadrants = {
            {"NE (Quadrant 0)",  0.1,  0.1},
            {"NW (Quadrant 1)", -0.1,  0.1},
            {"SW (Quadrant 2)", -0.1, -0.1},
            {"SE (Quadrant 3)",  0.1, -0.1}
        };

        long previousTotalEvents = parent.getTotalPublicationProcessingEvents();

        // 5. Loop through each quadrant and verify independence
        for (Object[] qData : testQuadrants) {
            String qName = (String) qData[0];
            double offX = (double) qData[1];
            double offY = (double) qData[2];

            Location pubLoc = new Location(cx + offX, cy + offY, 0);

            logger.info("--- Testing " + qName + " ---");
            
            // Send Burst
            for (int i = 0; i < BURST_SIZE; i++) {
                PublicationWithLocation pub = new PublicationWithLocation(pubLoc);
                proxLeaf.matchPublication(pub);
            }

            // Check Results
            long currentTotalEvents = parent.getTotalPublicationProcessingEvents();
            long processedInBatch = currentTotalEvents - previousTotalEvents;
            previousTotalEvents = currentTotalEvents; // Update baseline for next loop

            logger.info(qName + ": Sent " + BURST_SIZE + ", Parent processed " + processedInBatch);

            // Assertions
            if (processedInBatch != BRAKE_LIMIT) {
                logger.severe("FAILURE: " + qName + " failed. Expected " + BRAKE_LIMIT + " passed, but got " + processedInBatch);
                // If we get 0, it means the counters are NOT independent (shared pool exhausted).
                if (processedInBatch == 0) {
                    logger.severe("       (Result 0 implies quadrants are sharing a single counter!)");
                }
                return false;
            }

            // Verify Blocking specifically for this quadrant now
            PublicationWithLocation extraPub = new PublicationWithLocation(pubLoc);
            proxLeaf.matchPublication(extraPub);
            
            if (parent.getTotalPublicationProcessingEvents() != previousTotalEvents) {
                logger.severe("FAILURE: " + qName + " did not permanently block after limit.");
                return false;
            }
        }

        logger.info("SUCCESS: All quadrants handled traffic independently and enforced limits.");
        return true;
    }

    private SimulationBroker findFirstLeaf(SimulationBroker node) {
        if (node.getChildren().isEmpty()) return node;
        for (simulator.core.TreeNode child : node.getChildren()) {
            if (child instanceof SimulationBroker) {
                return findFirstLeaf((SimulationBroker) child);
            }
        }
        return node;
    }
}