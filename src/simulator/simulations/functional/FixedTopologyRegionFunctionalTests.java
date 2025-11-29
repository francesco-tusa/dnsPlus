package simulator.simulations.functional;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.regions.Region;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.SubscriptionWithRegion;
import simulator.topology.analysis.TopologyAnalyzer; // Import
import utils.CustomLogger;

public class FixedTopologyRegionFunctionalTests {

    private static final Logger logger = CustomLogger.getLogger(FixedTopologyRegionFunctionalTests.class.getName());

    public static final Predicate<BoundedBroker> COMPREHENSIVE_SCENARIO = root -> {
        logger.info("\n>>> SCENARIO: Running Comprehensive Cross-Branch and Local Propagation Test. <<<");

        // Use TopologyAnalyzer
        SubscriberWithLocation s2 = TopologyAnalyzer.findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s8 = TopologyAnalyzer.findNodeByName(root, "sub8", SubscriberWithLocation.class);
        SubscriberWithLocation s5 = TopologyAnalyzer.findNodeByName(root, "sub5", SubscriberWithLocation.class);
        PublisherWithLocation p1 = TopologyAnalyzer.findNodeByName(root, "pub1", PublisherWithLocation.class);
        PublisherWithLocation p2 = TopologyAnalyzer.findNodeByName(root, "pub2", PublisherWithLocation.class);

        if (s2 == null || s8 == null || s5 == null || p1 == null || p2 == null) {
            logger.severe("TEST SETUP FAILURE: Could not find all required nodes for the comprehensive test.");
            return false;
        }

        s2.send(new SubscriptionWithRegion(new Region(new Location(16, 3, 0), new Location(19, 5, 0))));
        s8.send(new SubscriptionWithRegion(new Region(new Location(17, 3, 0), new Location(19, 5, 0))));
        s5.send(new SubscriptionWithRegion(new Region(new Location(0, 0, 0), new Location(2, 2, 0))));

        logger.info("\n--- Broker Subscription Tables State (Post-Subscription) ---");
        FunctionalTestUtils.printAllSubscriptionTables(root);
        logger.info(""); 

        p2.send(new PublicationWithLocation(p2.getLocation()));
        p1.send(new PublicationWithLocation(p1.getLocation()));

        logger.info("\n--- Final Check ---");
        
        boolean s2Success = s2.getnPublications() == 1;
        if (!s2Success) logger.severe("  - Subscriber 's2': FAILED. Expected 1, got " + s2.getnPublications());
        
        boolean s8Success = s8.getnPublications() == 1;
        if (!s8Success) logger.severe("  - Subscriber 's8': FAILED. Expected 1, got " + s8.getnPublications());
        
        boolean s5Success = s5.getnPublications() == 1;
        if (!s5Success) logger.severe("  - Subscriber 's5': FAILED. Expected 1, got " + s5.getnPublications());

        boolean success = s2Success && s8Success && s5Success;

        if (success) {
            logger.info("SUCCESS: The Comprehensive Scenario test passed.");
        } else {
            logger.severe("FAILED: The Comprehensive Scenario test did not pass.");
        }

        return success;
    };

    public static final Predicate<BoundedBroker> SUBSCRIPTION_COVERING_SCENARIO = root -> {
        logger.info("\n>>> SCENARIO: Running Subscription Covering Test (Large contains Small). <<<");

        SubscriberWithLocation s2 = TopologyAnalyzer.findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s3 = TopologyAnalyzer.findNodeByName(root, "sub3", SubscriberWithLocation.class);
        PublisherWithLocation p1 = TopologyAnalyzer.findNodeByName(root, "pub1", PublisherWithLocation.class);
        SpatialMatchBroker child2 = TopologyAnalyzer.findNodeByName(root, "child2", SpatialMatchBroker.class);

        if (s2 == null || s3 == null || p1 == null || child2 == null) return false;

        s2.send(new SubscriptionWithRegion(new Region(new Location(0, 0, 0), new Location(20, 20, 0))));
        s3.send(new SubscriptionWithRegion(new Region(new Location(5, 5, 0), new Location(10, 10, 0))));

        Map<TreeNode, List<SimulationSubscription>> propagatedSubs = child2.getPropagatedSubscriptions();
        long upwardPropagations = propagatedSubs.keySet().stream()
                .filter(node -> node == child2.getParentBroker())
                .count();

        logger.info("  - Broker 'child2' subscriptions table size: " + child2.getInputSubscriptionCount()); 
        logger.info("  - Broker 'child2' upward propagations: " + upwardPropagations);

        boolean filteringSuccess = (upwardPropagations == 1);

        p1.send(new PublicationWithLocation(new Location(7, 7, 0)));
        boolean deliverySuccess = s2.getnPublications() == 1 && s3.getnPublications() == 1;

        return filteringSuccess && deliverySuccess;
    };

    public static final Predicate<BoundedBroker> SUBSCRIPTION_EXPANSION_SCENARIO = root -> {
        logger.info("\n>>> SCENARIO: Running Subscription Region EXPANSION Test. <<<");

        SubscriberWithLocation s2 = TopologyAnalyzer.findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s3 = TopologyAnalyzer.findNodeByName(root, "sub3", SubscriberWithLocation.class);
        SpatialMatchBroker child2 = TopologyAnalyzer.findNodeByName(root, "child2", SpatialMatchBroker.class);
        TreeNode grandchild2 = TopologyAnalyzer.findNodeByName(root, "grandchild2", TreeNode.class);
        TreeNode grandchild3 = TopologyAnalyzer.findNodeByName(root, "grandchild3", TreeNode.class);

        if (s2 == null || s3 == null || child2 == null) {
            logger.severe("TEST SETUP FAILURE: Could not find required nodes.");
            return false;
        }

        Region regionA = new Region(new Location(0, 0, 0), new Location(5, 5, 0));
        Region regionB = new Region(new Location(10, 10, 0), new Location(15, 15, 0));
        Region regionC = new Region(new Location(20, 20, 0), new Location(25, 25, 0));
        
        Region expectedMainTableRegion_S2 = new Region(regionA); expectedMainTableRegion_S2.expand(regionC);
        Region expectedMainTableRegion_S3 = new Region(regionB);
        Region expectedPropagatedRegion = new Region(regionA); expectedPropagatedRegion.expand(regionB); expectedPropagatedRegion.expand(regionC);

        s2.send(new SubscriptionWithRegion(regionA));
        s3.send(new SubscriptionWithRegion(regionB));
        s2.send(new SubscriptionWithRegion(regionC));

        Map<TreeNode, List<SimulationSubscription>> mainTable = child2.getInputSubscriptions();
        List<SimulationSubscription> g2Subs = mainTable.get(grandchild2);
        List<SimulationSubscription> g3Subs = mainTable.get(grandchild3);
        
        boolean g2Correct = false;
        if (g2Subs != null && !g2Subs.isEmpty()) {
            SubscriptionWithRegion sub = (SubscriptionWithRegion) g2Subs.get(0);
            g2Correct = sub.getRegion().equals(expectedMainTableRegion_S2);
            if (!g2Correct) logger.severe("FAILURE (Main Table S2): Expected " + expectedMainTableRegion_S2.toShortString() + " but got " + sub.getRegion().toShortString());
        } else {
            logger.severe("FAILURE (Main Table S2): No subscription found for Grandchild 2");
        }
        
        boolean g3Correct = false;
        if (g3Subs != null && !g3Subs.isEmpty()) {
            SubscriptionWithRegion sub = (SubscriptionWithRegion) g3Subs.get(0);
            g3Correct = sub.getRegion().equals(expectedMainTableRegion_S3);
            if (!g3Correct) logger.severe("FAILURE (Main Table S3): Expected " + expectedMainTableRegion_S3.toShortString() + " but got " + sub.getRegion().toShortString());
        } else {
            logger.severe("FAILURE (Main Table S3): No subscription found for Grandchild 3");
        }

        Map<TreeNode, List<SimulationSubscription>> propagatedSubs = child2.getPropagatedSubscriptions();
        List<SimulationSubscription> rootSubs = propagatedSubs.get(root);
        
        boolean expansionSuccess = false;
        if (rootSubs != null && !rootSubs.isEmpty()) {
            SubscriptionWithRegion propagatedToRoot = (SubscriptionWithRegion) rootSubs.get(0);
            expansionSuccess = propagatedToRoot.getRegion().equals(expectedPropagatedRegion);
            if (!expansionSuccess) logger.severe("FAILURE (Propagation): Expected " + expectedPropagatedRegion.toShortString() + " but got " + propagatedToRoot.getRegion().toShortString());
        } else {
            logger.severe("FAILURE (Propagation): No subscription propagated to Root");
        }

        return g2Correct && g3Correct && expansionSuccess;
    };
}