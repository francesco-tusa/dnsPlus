package simulator.tests.scenarios.proximity.e2e;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SimulationBroker;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.tests.framework.FunctionalTestUtils;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;
import utils.CustomLogger;

/**
 * End-to-End Sanity Check for Proximity Routing on the Simple Fixed Topology.
 * Uses floating point coordinates and dynamic actor creation.
 */
public class ProximityFixedTopologyE2ETest extends TestScenario {

    private static final Logger logger = CustomLogger.getLogger(ProximityFixedTopologyE2ETest.class.getName());

    @Override
    public String getTestName() { 
        return "Proximity E2E: Fixed Topology (Floating Point)"; 
    }

    @Override
    public boolean run(TopologyFixture fixture) {
        // 1. Locate Brokers (Topology containers)
        SimulationBroker brokerChild1 = fixture.findNode("child1", SimulationBroker.class);
        SimulationBroker brokerChild2 = fixture.findNode("child2", SimulationBroker.class);

        if (brokerChild1 == null || brokerChild2 == null) {
            logger.severe("Topology Error: Could not find required brokers 'child1' or 'child2'");
            return false;
        }

        // 2. Create & Attach Actors (Dynamic Topology Modification)
        // Subscriber at (10.5, 10.5)
        Location locSub = new Location(10.5, 10.5, 0);
        SubscriberWithLocation sub = new SubscriberWithLocation("ManualSub", locSub);
        brokerChild1.addChild(sub);

        // Publisher Local at (10.5, 10.5) - Same location (Dist=0.0)
        Location locClose = new Location(10.5, 10.5, 0);
        PublisherWithLocation pubLocal = new PublisherWithLocation("PubLocal", locClose);
        brokerChild1.addChild(pubLocal);

        // Publisher Remote at (100.123, 10.5) - Far away
        Location locFar = new Location(100.123, 10.5, 0);
        PublisherWithLocation pubRemote = new PublisherWithLocation("PubRemote", locFar);
        brokerChild2.addChild(pubRemote);

        // 3. Subscribe
        sub.send(new SubscriptionWithLocation(locSub));

        // 4. Publish Sequence
        
        // Step A: Publish from FAR (accepted as first)
        pubRemote.send(new PublicationWithLocation(locFar));
        
        // Step B: Publish from CLOSE (accepted as improvement)
        pubLocal.send(new PublicationWithLocation(locClose));

        // Step C: Publish from FAR AGAIN (rejected as worse)
        pubRemote.send(new PublicationWithLocation(locFar));

        // 5. Verify
        try {
            FunctionalTestUtils.assertReceived(sub, 2); 
            logger.info("Success: Subscriber received exactly 2 publications (Far + Close), 3rd was filtered.");
        } catch (AssertionError e) {
            logger.severe("Failure: " + e.getMessage());
            return false;
        }
        return true;
    }
}