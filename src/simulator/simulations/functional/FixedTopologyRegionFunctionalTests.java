package simulator.simulations.functional;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.logging.Logger; // Import Logger
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.regions.BrokerWithRegion;
import simulator.regions.BrokerWithRegionProcessingRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;
import utils.CustomLogger; // Import CustomLogger

public class FixedTopologyRegionFunctionalTests {

    private static final Logger logger = CustomLogger.getLogger(FixedTopologyRegionFunctionalTests.class.getName()); // Get logger

    public static final Predicate<BrokerWithRegion> COMPREHENSIVE_SCENARIO = root -> {
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
     * prevent redundant upward subscription propagation, while still ensuring 
     * correct delivery.
     */
    public static final Predicate<BrokerWithRegion> SUBSCRIPTION_COVERING_SCENARIO = root -> {
        logger.info(
                "\n>>> SCENARIO: Running Subscription Covering Test with Explicit Propagation Table Verification. <<<");

        // --- Find required nodes ---
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s3 = findNodeByName(root, "sub3", SubscriberWithLocation.class);
        PublisherWithLocation p1 = findNodeByName(root, "pub1", PublisherWithLocation.class);
        BrokerWithRegionProcessingRegion child2 = findNodeByName(root, "child2",
                BrokerWithRegionProcessingRegion.class);

        if (s2 == null || s3 == null || p1 == null || child2 == null) {
            logger.severe("Test failed: Could not find required nodes for the test.");
            return false;
        }

        // --- Send Subscriptions ---
        s2.send(new SubscriptionWithRegion(new Region(new Location(0, 0, 0), new Location(20, 20, 0))));
        s3.send(new SubscriptionWithRegion(new Region(new Location(5, 5, 0), new Location(10, 10, 0))));

        // --- Verification Part 1: ---
        // We get the propagatedSubscriptions table from child2.
        Map<TreeNode, SimulationSubscription> propagatedSubs = child2.getPropagatedSubscriptions();

        // We count how many times a subscription
        // was propagated to the parent broker. This should only happen once.
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
        Location publicationLocation = new Location(7, 7, 0);
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
            logger.info("\n--- Validation Result ---");
            logger.info("FAILED: The Subscription Covering test did not pass. Filtering success: "
                    + filteringSuccess + ", Delivery success: " + deliverySuccess);
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