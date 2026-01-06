// src/simulator/tests/scenarios/SubscriptionExpansionTest.java
package simulator.tests.scenarios;

import java.util.List;
import java.util.Map;
import simulator.core.TreeNode;
import simulator.events.SimulationSubscription;
import simulator.regions.Region;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SubscriptionWithRegion;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

public class SubscriptionExpansionTest extends TestScenario {
    @Override
    public String getTestName() { return "Subscription Expansion & Merging"; }

    @Override
    public boolean run(TopologyFixture fixture) {
        var s2 = requireSubscriber(fixture, "sub2");
        var s3 = requireSubscriber(fixture, "sub3");
        var child2 = fixture.findNode("child2", SpatialMatchBroker.class);
        var grandchild2 = fixture.findNode("grandchild2", TreeNode.class);
        
        // Regions setup
        Region regionA = new Region(new simulator.core.Location(0, 0, 0), new simulator.core.Location(5, 5, 0));
        Region regionB = new Region(new simulator.core.Location(10, 10, 0), new simulator.core.Location(15, 15, 0));
        Region regionC = new Region(new simulator.core.Location(20, 20, 0), new simulator.core.Location(25, 25, 0));
        
        // Expected Union (for Simple Strategy)
        Region unionAC = new Region(regionA); unionAC.expand(regionC);

        // Send split regions from same subscriber
        s2.send(new SubscriptionWithRegion(regionA));
        s3.send(new SubscriptionWithRegion(regionB));
        s2.send(new SubscriptionWithRegion(regionC));

        // Verify Input Store for Grandchild2 (Where s2 is connected)
        Map<TreeNode, List<SimulationSubscription>> mainTable = child2.getInputSubscriptions();
        List<SimulationSubscription> g2Subs = mainTable.get(grandchild2);

        if (g2Subs == null || g2Subs.isEmpty()) {
            logger.severe("FAILURE: No subscription found for Grandchild 2");
            return false;
        }

        // FLEXIBLE ASSERTION: Check if we have the Union OR both components
        boolean hasUnion = false;
        boolean hasA = false;
        boolean hasC = false;

        for (SimulationSubscription s : g2Subs) {
            if (s instanceof SubscriptionWithRegion swr) {
                if (swr.getRegion().equals(unionAC)) hasUnion = true;
                if (swr.getRegion().contains(regionA)) hasA = true;
                if (swr.getRegion().contains(regionC)) hasC = true;
            }
        }

        if (hasUnion) {
            // Passed (Simple Strategy behavior)
        } else if (hasA && hasC) {
            // Passed (Smart Strategy behavior - refused to merge)
        } else {
            logger.severe("FAILURE (Expansion): Expected Union " + unionAC.toShortString() 
                + " OR separate regions A & C. Found: " + g2Subs.size() + " entries.");
            return false;
        }

        return true;
    }
}