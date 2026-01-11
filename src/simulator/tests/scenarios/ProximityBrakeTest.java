package simulator.tests.scenarios;

import simulator.core.Location;
import simulator.entities.SimulationBroker;
import simulator.events.PublicationWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.ProximityRoutingLeafBroker;
import simulator.regions.policy.DecayingCounterBrakeStrategy;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

/**
 * Verifies the "Brake Rule" from the Heavy Version (Closest) algorithm.
 * * Logic Tested:
 * 1. Configures a Leaf Broker with a Brake Strategy (Limit=5, Interval=1000ms).
 * 2. Simulates a burst of publications.
 * 3. Asserts that the Parent Broker's processing counter increases by exactly 5.
 * 4. Advances time and asserts the counter increases again.
 */
public class ProximityBrakeTest extends TestScenario {

    private static final int BRAKE_LIMIT = 5;
    private static final long BRAKE_INTERVAL_MS = 1000;

    @Override
    public String getTestName() {
        return "Proximity Brake Logic Test";
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

        logger.info("Testing Brake on Broker: " + proxLeaf.getName() + " -> Parent: " + parent.getName());

        // 2. Inject Brake Strategy
        DecayingCounterBrakeStrategy strategy = new DecayingCounterBrakeStrategy(BRAKE_LIMIT, BRAKE_INTERVAL_MS);
        proxLeaf.setBrakeStrategy(strategy);

        // 3. Setup Subscription
        // Note: Using proxLeaf.getRegion() as discussed previously
        SubscriptionWithLocation sub = new SubscriptionWithLocation(proxLeaf.getRegion().getCenter());
        sub.setSource(parent);
        proxLeaf.addSubscription(sub);

        // 4. Phase 1: The Burst (Time T=0)
        Location center = proxLeaf.getRegion().getCenter();
        Location northEastLoc = new Location(center.getX() + 1.0, center.getY() + 1.0, 0);

        logger.info("--- Phase 1: Sending burst of 10 pubs (Limit " + BRAKE_LIMIT + ") ---");
        
        // Capture baseline metric from the Parent
        long initialParentCount = parent.getTotalPublicationProcessingEvents();
        
        for (int i = 0; i < 10; i++) {
            PublicationWithLocation pub = new PublicationWithLocation(northEastLoc);
            proxLeaf.matchPublication(pub); 
        }

        // Calculate how many reached the parent
        long afterBurstCount = parent.getTotalPublicationProcessingEvents();
        long receivedPhase1 = afterBurstCount - initialParentCount;
        
        logger.info("Parent processed " + receivedPhase1 + " new publications.");

        if (receivedPhase1 != BRAKE_LIMIT) {
            logger.severe("FAILURE: Brake failed. Expected " + BRAKE_LIMIT + " but Parent processed " + receivedPhase1);
            return false;
        }

        // 5. Phase 2: The Reset (Time T > 1000ms)
        logger.info("--- Phase 2: Waiting " + (BRAKE_INTERVAL_MS + 100) + "ms for token reset ---");
        
        try {
            Thread.sleep(BRAKE_INTERVAL_MS + 100);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        // Send 1 more pub. It should pass now.
        PublicationWithLocation pubAfterReset = new PublicationWithLocation(northEastLoc);
        proxLeaf.matchPublication(pubAfterReset);

        long finalCount = parent.getTotalPublicationProcessingEvents();
        long receivedPhase2 = finalCount - afterBurstCount;

        logger.info("Parent processed " + receivedPhase2 + " additional publications.");

        if (receivedPhase2 != 1) {
            logger.severe("FAILURE: Brake reset failed. Expected 1 passed message but got " + receivedPhase2);
            return false;
        }

        logger.info("SUCCESS: Proximity Brake Logic verified.");
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