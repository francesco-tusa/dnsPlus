package simulator.tests.scenarios;

import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

public class GridCrossCornerTest extends TestScenario {
    @Override
    public String getTestName() { return "Grid Cross-Corner Propagation"; }

    @Override
    public boolean run(TopologyFixture fixture) {
        // Grid 3x3 specific nodes
        var sub = requireSubscriber(fixture, "sub-0-0-0");
        var pub = requirePublisher(fixture, "pub-2-2-0");

        Location pubLoc = pub.getLocation();
        
        // Subscriber specifically targets the Publisher's location
        sub.send(new SubscriptionWithRegion(new Region(pubLoc, pubLoc)));
        pub.send(new PublicationWithLocation(pubLoc));

        try {
            assertReceived(sub, 1);
        } catch (AssertionError e) {
            logger.severe(e.getMessage());
            return false;
        }
        return true;
    }
}