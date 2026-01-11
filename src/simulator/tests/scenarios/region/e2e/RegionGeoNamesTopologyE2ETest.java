package simulator.tests.scenarios.region.e2e;

import java.util.List;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import simulator.tests.framework.FunctionalTestUtils;
import simulator.tests.framework.TestScenario;
import simulator.tests.framework.TopologyFixture;

public class RegionGeoNamesTopologyE2ETest extends TestScenario {

    @Override
    public String getTestName() { return "GeoNames Complex Propagation"; }

    @Override
    public boolean run(TopologyFixture fixture) {
        // ----------------------------------------------------------------
        // 1. ACQUIRE ACTORS
        // ----------------------------------------------------------------
        BoundedBroker dhaka = fixture.findNodeContains("Dhaka", BoundedBroker.class);
        BoundedBroker sylhet = fixture.findNodeContains("Sylhet", BoundedBroker.class);
        BoundedBroker bangladesh = fixture.findNodeContains("Bangladesh", BoundedBroker.class);
        BoundedBroker chittagong = fixture.findNodeContains("Chittagong", BoundedBroker.class);

        if (bangladesh == null || chittagong == null) {
            logger.warning("Skipping Test: Required brokers not found.");
            return true;
        }

        // Subscribers & Publishers
        var subLocal = FunctionalTestUtils.requireSubscriber(fixture, "Sub-Dhaka-Local");
        var subRemote = FunctionalTestUtils.requireSubscriber(fixture, "Sub-Dhaka-Remote");
        var subRajshahi = FunctionalTestUtils.requireSubscriber(fixture, "Sub-Rajshahi");
        var subChitA = FunctionalTestUtils.requireSubscriber(fixture, "Sub-Chit-A");
        var subChitB = FunctionalTestUtils.requireSubscriber(fixture, "Sub-Chit-B");
        var subDhakaSmall = FunctionalTestUtils.requireSubscriber(fixture, "Sub-Dhaka-Small");

        var pubSylhet = FunctionalTestUtils.requirePublisher(fixture, "Pub-Sylhet");
        var pubTibet = FunctionalTestUtils.requirePublisher(fixture, "Pub-Tibet");
        var pubBeijing = FunctionalTestUtils.requirePublisher(fixture, "Pub-Beijing");
        var pubRajshahi = FunctionalTestUtils.requirePublisher(fixture, "Pub-Rajshahi");

        boolean allPassed = true;

        // ----------------------------------------------------------------
        // PHASE 1: Downward Intersection
        // ----------------------------------------------------------------
        Region regionSylhet = new Region(new Location(91.5, 23.0, 0), new Location(92.0, 24.0, 0));
        subLocal.send(new SubscriptionWithRegion(regionSylhet));

        // Validation: Verify Sylhet received a subscription covering the request
        if (!verifySubscriptionCoverage(sylhet, bangladesh, regionSylhet.getBottomLeft())) {
             logger.severe("FAILURE: Sylhet did not receive subscription from Bangladesh."); 
             allPassed = false; 
        }

        // ----------------------------------------------------------------
        // PHASE 1.B: Downward Redundancy
        // ----------------------------------------------------------------
        Region smallSylhet = new Region(new Location(91.6, 23.2, 0), new Location(91.8, 23.8, 0));
        long sylhetAddedBefore = sylhet.getSubAddedCount();
        
        subRajshahi.send(new SubscriptionWithRegion(smallSylhet)); // Redundant
        
        if (sylhet.getSubAddedCount() != sylhetAddedBefore) {
            logger.severe("FAILURE: Redundant subscription leaked to Sylhet!");
            allPassed = false;
        }

        // ----------------------------------------------------------------
        // PHASE 2: Aggregation & Filtering (SMART/SIMPLE COMPATIBLE)
        // ----------------------------------------------------------------
        Region regionA = new Region(new Location(116.0, 39.0, 0), new Location(116.1, 39.1, 0));
        Region regionB = new Region(new Location(116.9, 39.9, 0), new Location(117.0, 40.0, 0));
        
        subChitA.send(new SubscriptionWithRegion(regionA));
        subChitB.send(new SubscriptionWithRegion(regionB));

        List<SimulationSubscription> chitPropagated = chittagong.getPropagatedSubscriptions().get(bangladesh);
        
        boolean coversA = false;
        boolean coversB = false;

        if (chitPropagated != null) {
            for (SimulationSubscription sub : chitPropagated) {
                if (sub instanceof SubscriptionWithRegion swr) {
                    if (swr.getRegion().contains(regionA)) coversA = true;
                    if (swr.getRegion().contains(regionB)) coversB = true;
                }
            }
        }

        if (coversA && coversB) {
            // Success: Either they merged into one big region, or kept as two small ones.
            // Both are valid behaviors.
        } else {
            logger.severe("FAILURE Phase 2: Propagation did not cover both regions. A=" + coversA + ", B=" + coversB);
            allPassed = false;
        }

        // Check Filtering (Upstream)
        Region largeBeijing = new Region(new Location(115.0, 38.0, 0), new Location(118.0, 42.0, 0)); 
        subRemote.send(new SubscriptionWithRegion(largeBeijing));
        
        // Snapshot state
        Region state1 = getFirstPropagatedRegion(dhaka, bangladesh);
        
        Region smallBeijing = new Region(new Location(116.4, 39.9, 0), new Location(116.5, 40.0, 0));
        subDhakaSmall.send(new SubscriptionWithRegion(smallBeijing)); // Should be filtered

        Region state2 = getFirstPropagatedRegion(dhaka, bangladesh);
        
        // Logic: Whether Simple or Smart, adding a 'contained' region should not change the MBR/List logic drastically
        // For Simple: MBR doesn't change.
        // For Smart: smallBeijing is absorbed by largeBeijing.
        if (state1 == null || !state1.equals(state2)) { 
            logger.severe("FAILURE Phase 2: Filtering failed (Propagated region changed unexpectedly)."); 
            allPassed = false; 
        }

        // ----------------------------------------------------------------
        // PHASE 3: Internal Routing
        // ----------------------------------------------------------------
        pubSylhet.send(new PublicationWithLocation(new Location(91.6, 23.5, 0)));
        if (subLocal.getnPublications() != 1 || subRemote.getnPublications() != 0) {
            logger.severe("FAILURE Phase 3: Internal routing mismatch.");
            allPassed = false;
        }

        // ----------------------------------------------------------------
        // PHASE 4: Outlier Test
        // ----------------------------------------------------------------
        pubTibet.send(new PublicationWithLocation(new Location(100.0, 30.0, 0)));
        if (subLocal.getnPublications() != 1) { 
            logger.severe("FAILURE Phase 4: Subscriber received outlier!");
            allPassed = false;
        }

        // ----------------------------------------------------------------
        // PHASE 5: Remote Propagation
        // ----------------------------------------------------------------
        pubBeijing.send(new PublicationWithLocation(new Location(116.0, 40.0, 0)));
        if (subLocal.getnPublications() != 1 || subRemote.getnPublications() != 1) {
            logger.severe("FAILURE Phase 5: Remote publication delivery failed.");
            allPassed = false;
        }
        
        // ----------------------------------------------------------------
        // STEP 6: Nearby Branch Routing
        // ----------------------------------------------------------------
        Location rajshahiPoint = pubRajshahi.getLocation();
        Region regionRajshahi = new Region(
            new Location(rajshahiPoint.getX() - 0.1, rajshahiPoint.getY() - 0.1, 0),
            new Location(rajshahiPoint.getX() + 0.1, rajshahiPoint.getY() + 0.1, 0)
        );
        subLocal.send(new SubscriptionWithRegion(regionRajshahi));
        pubRajshahi.send(new PublicationWithLocation(rajshahiPoint));
        
        if (subLocal.getnPublications() != 2) {
            logger.severe("FAILURE Phase 6: Nearby Branch Routing failed.");
            allPassed = false;
        }

        return allPassed;
    }
    
    // --- Helpers ---

    private boolean verifySubscriptionCoverage(BoundedBroker receiver, BoundedBroker sender, Location target) {
        List<SimulationSubscription> subs = receiver.getInputSubscriptions().get(sender);
        if (subs == null) return false;
        for (SimulationSubscription s : subs) {
            if (s instanceof SubscriptionWithRegion sub && sub.getRegion().contains(target)) {
                return true;
            }
        }
        return false;
    }

    private Region getFirstPropagatedRegion(BoundedBroker source, BoundedBroker target) {
        List<SimulationSubscription> subs = source.getPropagatedSubscriptions().get(target);
        if (subs != null && !subs.isEmpty()) {
            if (subs.get(0) instanceof SubscriptionWithRegion swr) return swr.getRegion();
        }
        return null;
    }
}