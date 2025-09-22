package simulator.simulations.functional;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.BrokerWithRegion;

/**
 * A utility class containing static validation tests specifically for the Fixed
 * Topology when using LOCATION-based brokers.
 */
public class FixedTopologyLocationFunctionalTests {

    /**
     * A comprehensive test to verify the core "improvement" filtering logic at a
     * leaf broker, including cross-branch propagation. A subscriber receives three 
     * publications and should only accept the ones that are closer than what it 
     * has previously received.
     */
    public static final Predicate<BrokerWithRegion> COMPREHENSIVE_SCENARIO = root -> {
        System.out.println("\n>>> SCENARIO: Testing Comprehensive Publication Improvement Filtering (Location). <<<");
        SubscriberWithLocation s8 = findNodeByName(root, "sub8", SubscriberWithLocation.class);
        PublisherWithLocation p2 = findNodeByName(root, "pub2", PublisherWithLocation.class);

        if (s8 == null || p2 == null) {
            System.err.println("Test failed: Could not find required nodes (sub8, pub2).");
            return false;
        }

        // s8 subscribes with its location.
        s8.send(new SubscriptionWithLocation(s8.getLocation()));

        System.out.println("\n--- Broker Subscription Tables State (Post-Subscription) ---");
        FunctionalTestUtils.printAllSubscriptionTables(root);
        System.out.println();

        // --- Send multiple publications ---
        // 1. A first publication from the publisher's default location. This should be
        // received.
        System.out.println("Sending first publication from " + p2.getLocation());
        p2.send(new PublicationWithLocation(p2.getLocation()));

        // 2. A second, farther away publication. This should be filtered out by the
        // leaf broker.
        Location fartherLocation = new Location(25, 10, 0); // Farther from s8 at (20, 5, 0)
        System.out.println("Sending second, farther publication from " + fartherLocation);
        p2.send(new PublicationWithLocation(fartherLocation));

        // 3. A third, closer publication. This should be an improvement and be
        // delivered.
        Location closerLocation = new Location(19.5, 5.0, 0); // Closer to s8
        System.out.println("Sending third, closer publication from " + closerLocation);
        p2.send(new PublicationWithLocation(closerLocation));

        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber 's8' received: " + s8.getnPublications() + " publications. (Expected: 2)");

        boolean success = s8.getnPublications() == 2;

        if (success) {
            System.out.println("\n--- Validation Result ---");
            System.out.println(
                    "SUCCESS: The Comprehensive Scenario (Location) test passed. Correctly received 2 of 3 publications.");
        } else {
            System.out.println("\n--- Validation Result ---");
            System.out.println("FAILED: The Comprehensive Scenario (Location) test did not pass.");
        }

        return success;
    };

    /**
     * Validates proxy subscription filtering where two different child brokers
     * cause a redundant subscription at their common parent, and then verifies
     * correct end-to-end publication delivery.
     */
    public static final Predicate<BrokerWithRegion> SUBSCRIPTION_FILTERING_SCENARIO = root -> {
        System.out.println(
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
            System.err.println("Test failed: Could not find all required nodes for the comprehensive test.");
            return false;
        }

        // --- Phase 1: First Branch Subscription ---
        System.out.println("\n--- Phase 1: Subscription from the first branch ---");
        System.out.println(s2.getName() + " (under " + grandchild2.getName() + ") sends its subscription.");
        s2.send(new SubscriptionWithLocation(s2.getLocation()));

        // --- Phase 2: Second Branch Subscription & Upper-Level Filtering ---
        // s3 is under a different leaf broker (grandchild3), but its proxy will be
        // identical from the perspective of their common parent (child2).
        System.out.println("\n--- Phase 2: Subscription from a second branch to trigger filtering ---");
        System.out.println(s3.getName() + " (under " + grandchild3.getName() + ") sends its subscription.");
        s3.send(new SubscriptionWithLocation(s3.getLocation()));

        // --- Verification Part 1: Check Subscription Tables ---
        // child2 should have received two subscriptions (one from each child),
        // but should have only propagated ONE proxy to the root.
        int intermediateBrokerSubscriptionCount = child2.getSubscriptionsTable().size();
        long rootSubscriptionsFromChild2 = root.getSubscriptionsTable().keySet().stream()
                .filter(node -> node.getName().equals("child2"))
                .count();

        System.out.println("\n--- Mid-point Check ---");
        System.out.println("  - Intermediate broker 'child2' table size: " + intermediateBrokerSubscriptionCount
                + " (Expected: 2)");
        System.out.println("  - Root broker subscriptions originating from 'child2': " + rootSubscriptionsFromChild2
                + " (Expected: 1)");

        boolean filteringSuccess = (intermediateBrokerSubscriptionCount == 2) && (rootSubscriptionsFromChild2 == 1);

        System.out.println("\n--- Broker Subscription Tables State (Post-Subscription) ---");
        FunctionalTestUtils.printAllSubscriptionTables(root);
        System.out.println();


        // --- Phase 3: Publication and Delivery ---
        System.out.println("\n--- Phase 3: Testing Publication Delivery to both branches ---");
        System.out.println(p2.getName() + " sends a publication from " + p2.getLocation());
        p2.send(new PublicationWithLocation(p2.getLocation()));

        // --- Verification Part 2: Check Delivery ---
        System.out.println("\n--- Final Check ---");
        System.out.println(
                "  - Subscriber 's2' (Branch 1) received: " + s2.getnPublications() + " publications. (Expected: 1)");
        System.out.println(
                "  - Subscriber 's3' (Branch 2) received: " + s3.getnPublications() + " publications. (Expected: 1)");

        boolean deliverySuccess = s2.getnPublications() == 1 && s3.getnPublications() == 1;
        boolean finalSuccess = filteringSuccess && deliverySuccess;

        if (finalSuccess) {
            System.out.println("\n--- Validation Result ---");
            System.out.println(
                    "SUCCESS: The Upper-Level Proxy Filtering test passed. Filtering and delivery were correct.");
        } else {
            System.out.println("\n--- Validation Result ---");
            System.out.println("FAILED: The Upper-Level Proxy Filtering test did not pass.");
            System.out.println("  - Filtering success: " + filteringSuccess);
            System.out.println("  - Delivery success: " + deliverySuccess);
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