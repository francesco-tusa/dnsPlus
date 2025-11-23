package simulator.simulations.functional;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BoundedBroker;
import simulator.regions.SpatialMatchBroker;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import utils.CustomLogger;

public class FixedTopologyRegionFunctionalTests {

    private static final Logger logger = CustomLogger.getLogger(FixedTopologyRegionFunctionalTests.class.getName());

    public static final Predicate<BoundedBroker> COMPREHENSIVE_SCENARIO = root -> {
        logger.info("\n>>> SCENARIO: Running Comprehensive Cross-Branch and Local Propagation Test. <<<");

        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s8 = findNodeByName(root, "sub8", SubscriberWithLocation.class);
        SubscriberWithLocation s5 = findNodeByName(root, "sub5", SubscriberWithLocation.class);
        PublisherWithLocation p1 = findNodeByName(root, "pub1", PublisherWithLocation.class);
        PublisherWithLocation p2 = findNodeByName(root, "pub2", PublisherWithLocation.class);

        if (s2 == null || s8 == null || s5 == null || p1 == null || p2 == null) {
            logger.severe("Test failed: Could not find all required nodes for the comprehensive test.");
            return false;
        }

        s2.send(new SubscriptionWithRegion(new Region(new Location(16, 3, 0), new Location(19, 5, 0))));
        s8.send(new SubscriptionWithRegion(new Region(new Location(17, 3, 0), new Location(19, 5, 0))));
        s5.send(new SubscriptionWithRegion(new Region(new Location(0, 0, 0), new Location(2, 2, 0))));

        logger.info("\n--- Broker Subscription Tables State (Post-Subscription) ---");
        FunctionalTestUtils.printAllSubscriptionTables(root);
        logger.info(""); // Add newline

        p2.send(new PublicationWithLocation(p2.getLocation()));
        p1.send(new PublicationWithLocation(p1.getLocation()));

        logger.info("\n--- Final Check ---");
        logger.info("  - Subscriber 's2' received: " + s2.getnPublications() + " publications. (Expected: 1)");
        logger.info("  - Subscriber 's8' received: " + s8.getnPublications() + " publications. (Expected: 1)");
        logger.info("  - Subscriber 's5' received: " + s5.getnPublications() + " publications. (Expected: 1)");

        boolean success = s2.getnPublications() == 1 && s8.getnPublications() == 1 && s5.getnPublications() == 1;

        if (success) {
            logger.info("\n--- Validation Result ---");
            logger.info("SUCCESS: The Comprehensive Scenario test passed.");
        } else {
            logger.info("\n--- Validation Result ---");
            logger.info("FAILED: The Comprehensive Scenario test did not pass.");
        }

        return success;
    };

    /**
     * Validates that the `propagatedSubscriptions` table is correctly used to
     * prevent redundant upward subscription propagation (when a new sub is *contained* by an old one).
     */
    public static final Predicate<BoundedBroker> SUBSCRIPTION_COVERING_SCENARIO = root -> {
        logger.info(
                "\n>>> SCENARIO: Running Subscription Covering Test (Large contains Small). <<<");

        // --- Find required nodes ---
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s3 = findNodeByName(root, "sub3", SubscriberWithLocation.class);
        PublisherWithLocation p1 = findNodeByName(root, "pub1", PublisherWithLocation.class);
        SpatialMatchBroker child2 = findNodeByName(root, "child2",
                SpatialMatchBroker.class);

        if (s2 == null || s3 == null || p1 == null || child2 == null) {
            logger.severe("Test failed: Could not find required nodes for the test.");
            return false;
        }

        // --- Send Subscriptions ---
        // S2 sends a LARGE region
        s2.send(new SubscriptionWithRegion(new Region(new Location(0, 0, 0), new Location(20, 20, 0))));
        // S3 sends a SMALL region *inside* S2's region
        s3.send(new SubscriptionWithRegion(new Region(new Location(5, 5, 0), new Location(10, 10, 0))));

        // --- Verification Part 1: ---
        Map<TreeNode, SimulationSubscription> propagatedSubs = child2.getPropagatedSubscriptions();
        long upwardPropagations = propagatedSubs.keySet().stream()
                .filter(node -> node == child2.getParentBroker())
                .count();

        logger.info("\n--- Mid-point Check ---");
        logger.info("  - Broker 'child2' subscriptions table size: " + child2.getSubscriptionsTable().size()
                + " (Expected: 2)");
        logger.info(
                "  - Broker 'child2' upward propagations to parent: " + upwardPropagations + " (Expected: 1)");

        boolean filteringSuccess = (child2.getSubscriptionsTable().size() == 2) && (upwardPropagations == 1);

        logger.info("\n--- Broker Subscription Tables State (Post-Subscription) ---");
        FunctionalTestUtils.printAllSubscriptionTables(root);
        logger.info(""); // Add newline


        // --- Send Publication ---
        Location publicationLocation = new Location(7, 7, 0); // Inside both regions
        p1.send(new PublicationWithLocation(publicationLocation));

        // --- Verification Part 2: Check delivery ---
        logger.info("\n--- Final Check ---");
        logger.info("  - Subscriber 's2' received: " + s2.getnPublications() + " publications. (Expected: 1)");
        logger.info("  - Subscriber 's3' (whose sub was filtered) received: " + s3.getnPublications()
                + " publications. (Expected: 1)");

        boolean deliverySuccess = s2.getnPublications() == 1 && s3.getnPublications() == 1;
        boolean finalSuccess = filteringSuccess && deliverySuccess;

        if (finalSuccess) {
            logger.info("\n--- Validation Result ---");
            logger.info(
                    "SUCCESS: The Subscription Covering test passed. Propagation table and delivery were correct.");
        } else {
            logger.info("FAILED: The Subscription Covering test did not pass. Filtering success: "
                    + filteringSuccess + ", Delivery success: " + deliverySuccess);
        }

        return finalSuccess;
    };


    /**
     * Validates the "Region Expansion" (merging) logic for
     * BOTH the main subscription table (SimulationBroker.addSubscription)
     * AND the propagation filter table (BrokerWithRegionProcessingRegion.propagateOrExpandSubscription).
     * * This test sends:
     * 1. Region A from Source 1
     * 2. Region B from Source 2 (non-overlapping)
     * 3. Region C from Source 1 (non-overlapping with A or B)
     */
    public static final Predicate<BoundedBroker> SUBSCRIPTION_EXPANSION_SCENARIO = root -> {
        logger.info(
                "\n>>> SCENARIO: Running Subscription Region EXPANSION Test (Same and Different Sources). <<<");

        // --- Find required nodes ---
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class); // Under grandchild2
        SubscriberWithLocation s3 = findNodeByName(root, "sub3", SubscriberWithLocation.class); // Under grandchild3
        SpatialMatchBroker child2 = findNodeByName(root, "child2", // Common parent
                SpatialMatchBroker.class);
        TreeNode grandchild2 = findNodeByName(root, "grandchild2", TreeNode.class);
        TreeNode grandchild3 = findNodeByName(root, "grandchild3", TreeNode.class);


        if (s2 == null || s3 == null || child2 == null || grandchild2 == null || grandchild3 == null) {
            logger.severe("Test failed: Could not find required nodes for the expansion test.");
            return false;
        }

        // --- Define Test Regions ---
        Region regionA = new Region(new Location(0, 0, 0), new Location(5, 5, 0));   // From s2
        Region regionB = new Region(new Location(10, 10, 0), new Location(15, 15, 0)); // From s3
        Region regionC = new Region(new Location(20, 20, 0), new Location(25, 25, 0)); // From s2 again
        
        // --- Define Expected Regions ---
        
        // 1. For the main table:
        //    grandchild2's entry should be the union of A and C
        Region expectedMainTableRegion_S2 = new Region(regionA);
        expectedMainTableRegion_S2.expand(regionC); // Should be [0,0:25,25]
        
        //    grandchild3's entry should just be B
        Region expectedMainTableRegion_S3 = new Region(regionB); // Should be [10,10:15,15]


        // 2. For the filter table:
        //    The entry for 'root' should be the union of ALL propagated regions (A, B, and C)
        Region expectedPropagatedRegion = new Region(regionA);
        expectedPropagatedRegion.expand(regionB);
        expectedPropagatedRegion.expand(regionC); // Should be [0,0:25,25]


        // --- Send Subscriptions ---
        // 1. S2 (from grandchild2) sends Region A
        logger.info(String.format("%s sends first subscription for %s", s2.getName(), regionA.toShortString()));
        s2.send(new SubscriptionWithRegion(regionA));

        // 2. S3 (from grandchild3) sends Region B (non-overlapping)
        //    This tests expansion in the *propagatedSubscriptions* table for *different sources*
        logger.info(String.format("%s sends second, non-overlapping subscription for %s", s3.getName(), regionB.toShortString()));
        s3.send(new SubscriptionWithRegion(regionB));

        // 3. S2 (from grandchild2) sends Region C (non-overlapping with A)
        //    This tests expansion in *both* the *subscriptionsTable* (for the same source)
        //    and the *propagatedSubscriptions* table.
        logger.info(String.format("%s sends a second, non-overlapping subscription for %s", s2.getName(), regionC.toShortString()));
        s2.send(new SubscriptionWithRegion(regionC));


        // --- Verification ---
        logger.info("\n--- Final Check ---");
        
        // Check 1: The 'subscriptionsTable' of child2 (tests SimulationBroker.addSubscription)
        logger.info("--- Checking Main Subscription Table ('subscriptionsTable') of 'child2' ---");
        Map<TreeNode, SimulationSubscription> mainTable = child2.getSubscriptionsTable();
        SubscriptionWithRegion subFromGrandchild2 = (SubscriptionWithRegion) mainTable.get(grandchild2);
        SubscriptionWithRegion subFromGrandchild3 = (SubscriptionWithRegion) mainTable.get(grandchild3);

        boolean g2MainTableCorrect = false;
        if (subFromGrandchild2 != null && subFromGrandchild2.getRegion().equals(expectedMainTableRegion_S2)) {
            g2MainTableCorrect = true;
        }
        
        boolean g3MainTableCorrect = false;
        if (subFromGrandchild3 != null && subFromGrandchild3.getRegion().equals(expectedMainTableRegion_S3)) {
            g3MainTableCorrect = true;
        }

        boolean mainTableCorrect = g2MainTableCorrect && g3MainTableCorrect;
        logger.info("  - Main Table check: " + (mainTableCorrect ? "PASSED" : "FAILED"));
        logger.info(String.format("    -> Entry for %s: %s (Expected: %s)", 
            grandchild2.getName(), 
            subFromGrandchild2 != null ? subFromGrandchild2.getRegion().toShortString() : "NULL", 
            expectedMainTableRegion_S2.toShortString()));
        logger.info(String.format("    -> Entry for %s: %s (Expected: %s)", 
            grandchild3.getName(), 
            subFromGrandchild3 != null ? subFromGrandchild3.getRegion().toShortString() : "NULL", 
            expectedMainTableRegion_S3.toShortString()));


        // Check 2: The 'propagatedSubscriptions' table of child2 (tests BrokerWithRegionProcessingRegion.propagateOrExpandSubscription)
        logger.info("--- Checking Propagation Filter Table ('propagatedSubscriptions') of 'child2' ---");
        Map<TreeNode, SimulationSubscription> propagatedSubs = child2.getPropagatedSubscriptions();
        SubscriptionWithRegion propagatedToRoot = (SubscriptionWithRegion) propagatedSubs.get(root);

        boolean expansionSuccess = false;
        if (propagatedToRoot == null) {
            logger.info("  - Propagation check: FAILED ('child2' did not propagate any subscription to 'root')");
        } else {
            Region actualPropagatedRegion = propagatedToRoot.getRegion();

            logger.info("  - Expected propagated region (union of A, B, and C): " + expectedPropagatedRegion.toShortString());
            logger.info("  - Actual propagated region: " + actualPropagatedRegion.toShortString());

            // This is the critical check:
            // We verify that the final propagated region is the correct *expanded* region [0,0:25,25].
            expansionSuccess = actualPropagatedRegion.equals(expectedPropagatedRegion);
            logger.info("  - Propagation check: " + (expansionSuccess ? "PASSED" : "FAILED"));
        }


        boolean finalSuccess = mainTableCorrect && expansionSuccess;

        if (finalSuccess) {
            logger.info("\n--- Validation Result ---");
            logger.info("SUCCESS: The Subscription Expansion test passed. Regions were correctly merged in both tables.");
        } else {
            logger.info("\n--- Validation Result ---");
            logger.info("FAILED: The Subscription Expansion test did not pass.");
        }

        return finalSuccess;
    };


    private static <T extends TreeNode> T findNodeByName(TreeNode root, String name, Class<T> type) {
        if (root == null || name == null)
            return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (type.isInstance(current) && name.equals(current.getName())) {
                return type.cast(current);
            }
            if (current.getChildren() != null) {
                queue.addAll(current.getChildren());
            }
        }
        return null;
    }
}
