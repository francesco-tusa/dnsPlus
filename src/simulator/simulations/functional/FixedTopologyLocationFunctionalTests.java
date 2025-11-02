package simulator.simulations.functional;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.BrokerWithRegion;
import utils.CustomLogger;

public class FixedTopologyLocationFunctionalTests {

    private static final Logger logger = CustomLogger.getLogger(FixedTopologyLocationFunctionalTests.class.getName());

    /**
     * An comprehensive test to verify the "closest publication" logic.
     * It involves multiple subscribers and publishers across different branches
     * of the topology to demonstrate that the network correctly tracks the best
     * publication for each subscriber independently.
     */
    public static final Predicate<BrokerWithRegion> COMPREHENSIVE_SCENARIO = root -> {
        logger.info("\n>>> SCENARIO: Testing Multi-Client Closest Publication Filtering (Location). <<<");

        // --- Find required nodes ---
        SubscriberWithLocation sub2 = findNodeByName(root, "sub2", SubscriberWithLocation.class); // Location: (5, 2)
        SubscriberWithLocation sub8 = findNodeByName(root, "sub8", SubscriberWithLocation.class); // Location: (20, 5)
        PublisherWithLocation p1 = findNodeByName(root, "pub1", PublisherWithLocation.class); // Location: (7, 7)
        PublisherWithLocation p2 = findNodeByName(root, "pub2", PublisherWithLocation.class); // Location: (18, 4)

        if (sub2 == null || sub8 == null || p1 == null || p2 == null) {
            logger.severe("Test failed: Could not find all required nodes for the comprehensive test.");
            return false;
        }

        // --- Phase 1: Subscriptions ---
        sub2.send(new SubscriptionWithLocation(sub2.getLocation()));
        sub8.send(new SubscriptionWithLocation(sub8.getLocation()));

        // --- Phase 2: Publications (in a controlled order) ---
        // 1. Send initial, relevant publications to establish a baseline for each
        // subscriber.
        logger.info("\n--- Establishing Baselines ---");
        p1.send(new PublicationWithLocation(p1.getLocation())); // This is the baseline for sub2.
        p2.send(new PublicationWithLocation(p2.getLocation())); // This is the baseline for sub8.

        // 2. Send an improvement for sub2. This should be delivered to sub2 but
        // filtered for sub8.
        logger.info("\n--- Sending Improvement for sub2 ---");
        p1.send(new PublicationWithLocation(new Location(6.0, 3.0, 0.0)));

        // 3. Send an improvement for sub8. This should be delivered to sub8 but
        // filtered for sub2.
        logger.info("\n--- Sending Improvement for sub8 ---");
        p2.send(new PublicationWithLocation(new Location(19.0, 5.0, 0.0)));

        // 4. Send a publication that is NOT an improvement for either subscriber.
        logger.info("\n--- Sending Farther Publication (should be filtered) ---");
        p1.send(new PublicationWithLocation(new Location(15.0, 15.0, 0.0)));

        // --- Verification ---
        logger.info("\n--- Final Check ---");
        logger.info("  - Subscriber 'sub2' received: " + sub2.getnPublications() + " publications. (Expected: 2)");
        logger.info("  - Subscriber 'sub8' received: " + sub8.getnPublications() + " publications. (Expected: 3)");

        boolean success = sub2.getnPublications() == 2 && sub8.getnPublications() == 3;

        if (success) {
            logger.info("\n--- Validation Result ---");
            logger.info("SUCCESS: The Multi-Client Closest Publication test passed.");
        } else {
            logger.info("\n--- Validation Result ---");
            logger.info("FAILED: The Multi-Client Closest Publication test did not pass.");
        }

        return success;
    };

    /**
     * Validates proxy subscription filtering where two different child brokers
     * cause a redundant subscription at their common parent, and then verifies
     * correct end-to-end publication delivery.
     */
    public static final Predicate<BrokerWithRegion> SUBSCRIPTION_FILTERING_SCENARIO = root -> {
        logger.info(
                "\n>>> SCENARIO: Testing Upper-Level Proxy Subscription Filtering and Delivery (Location). <<<");

        // --- Find required nodes ---
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class); // Under grandchild2
        SubscriberWithLocation s3 = findNodeByName(root, "sub3", SubscriberWithLocation.class); // Under grandchild3
                                                                                                // (DIFFERENT leaf)

        // Brokers for verification
        BrokerWithRegion grandchild2 = findNodeByName(root, "grandchild2", BrokerWithRegion.class);
        BrokerWithRegion grandchild3 = findNodeByName(root, "grandchild3", BrokerWithRegion.class);
        BrokerWithRegion child2 = findNodeByName(root, "child2", BrokerWithRegion.class); // Common parent

        // Publisher for the test
        PublisherWithLocation p2 = findNodeByName(root, "pub2", PublisherWithLocation.class);

        if (s2 == null || s3 == null || grandchild2 == null || grandchild3 == null || child2 == null || p2 == null) {
            logger.severe("Test failed: Could not find all required nodes for the comprehensive test.");
            return false;
        }

        // --- Phase 1: First Branch Subscription ---
        logger.info("\n--- Phase 1: Subscription from the first branch ---");
        logger.info(s2.getName() + " (under " + grandchild2.getName() + ") sends its subscription.");
        s2.send(new SubscriptionWithLocation(s2.getLocation()));

        // --- Phase 2: Second Branch Subscription & Upper-Level Filtering ---
        // s3 is under a different leaf broker (grandchild3), but its proxy will be
        // identical from the perspective of their common parent (child2).
        logger.info("\n--- Phase 2: Subscription from a second branch to trigger filtering ---");
        logger.info(s3.getName() + " (under " + grandchild3.getName() + ") sends its subscription.");
        s3.send(new SubscriptionWithLocation(s3.getLocation()));

        // --- Verification Part 1: Check Subscription Tables ---
        // child2 should have received two subscriptions (one from each child),
        // but should have only propagated ONE proxy to the root.
        int intermediateBrokerSubscriptionCount = child2.getSubscriptionsTable().size();
        long rootSubscriptionsFromChild2 = root.getSubscriptionsTable().keySet().stream()
                .filter(node -> node.getName().equals("child2"))
                .count();

        logger.info("\n--- Mid-point Check ---");
        logger.info("  - Intermediate broker 'child2' table size: " + intermediateBrokerSubscriptionCount
                + " (Expected: 2)");
        logger.info("  - Root broker subscriptions originating from 'child2': " + rootSubscriptionsFromChild2
                + " (Expected: 1)");

        boolean filteringSuccess = (intermediateBrokerSubscriptionCount == 2) && (rootSubscriptionsFromChild2 == 1);

        logger.info("\n--- Broker Subscription Tables State (Post-Subscription) ---");
        FunctionalTestUtils.printAllSubscriptionTables(root);
        logger.info("");

        // --- Phase 3: Publication and Delivery ---
        logger.info("\n--- Phase 3: Testing Publication Delivery to both branches ---");
        logger.info(p2.getName() + " sends a publication from " + p2.getLocation());
        p2.send(new PublicationWithLocation(p2.getLocation()));

        // --- Verification Part 2: Check Delivery ---
        logger.info("\n--- Final Check ---");
        logger.info(
                "  - Subscriber 's2' (Branch 1) received: " + s2.getnPublications() + " publications. (Expected: 1)");
        logger.info(
                "  - Subscriber 's3' (Branch 2) received: " + s3.getnPublications() + " publications. (Expected: 1)");

        boolean deliverySuccess = s2.getnPublications() == 1 && s3.getnPublications() == 1;
        boolean finalSuccess = filteringSuccess && deliverySuccess;

        if (finalSuccess) {
            logger.info("\n--- Validation Result ---");
            logger.info(
                    "SUCCESS: The Upper-Level Proxy Filtering test passed. Filtering and delivery were correct.");
        } else {
            logger.info("\n--- Validation Result ---");
            logger.info("FAILED: The Upper-Level Proxy Filtering test did not pass.");
            logger.info("  - Filtering success: " + filteringSuccess);
            logger.info("  - Delivery success: " + deliverySuccess);
        }

        return finalSuccess;
    };

    // Helper method for finding named nodes in the topology
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
