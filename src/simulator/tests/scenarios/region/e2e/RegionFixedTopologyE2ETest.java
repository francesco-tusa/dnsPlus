// src/simulator/tests/scenarios/ComprehensiveFixedScenario.java
package simulator.tests.scenarios.region.e2e;

import simulator.core.Location;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.tests.framework.FunctionalTestUtils;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

public class RegionFixedTopologyE2ETest extends TestScenario {

    @Override
    public String getTestName() { return "Comprehensive Fixed Topology Check"; }

    @Override
    public boolean run(TopologyFixture fixture) {
        var s2 = FunctionalTestUtils.requireSubscriber(fixture, "sub2");
        var s8 = FunctionalTestUtils.requireSubscriber(fixture, "sub8");
        var s5 = FunctionalTestUtils.requireSubscriber(fixture, "sub5");
        var p1 = FunctionalTestUtils.requirePublisher(fixture, "pub1");
        var p2 = FunctionalTestUtils.requirePublisher(fixture, "pub2");

        // 1. Subscribe
        s2.send(new SubscriptionWithRegion(new Region(new Location(16, 3, 0), new Location(19, 5, 0))));
        s8.send(new SubscriptionWithRegion(new Region(new Location(17, 3, 0), new Location(19, 5, 0))));
        s5.send(new SubscriptionWithRegion(new Region(new Location(0, 0, 0), new Location(2, 2, 0))));

        // Debugging Helper
        FunctionalTestUtils.printAllSubscriptionTables(fixture.getRoot());

        // 2. Publish
        p2.send(new simulator.events.PublicationWithLocation(p2.getLocation()));
        p1.send(new simulator.events.PublicationWithLocation(p1.getLocation()));

        // 3. Verify
        try {
            FunctionalTestUtils.assertReceived(s2, 1);
            FunctionalTestUtils.assertReceived(s8, 1);
            FunctionalTestUtils.assertReceived(s5, 1);
        } catch (AssertionError e) {
            logger.severe("Failure: " + e.getMessage());
            return false;
        }
        return true;
    }
}