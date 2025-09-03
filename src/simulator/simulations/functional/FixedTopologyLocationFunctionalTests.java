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
 * A utility class containing static validation tests specifically for the Fixed Topology
 * when using LOCATION-based brokers.
 */
public class FixedTopologyLocationFunctionalTests {

    /**
     * A comprehensive test to verify the core "improvement" filtering logic at a leaf broker,
     * including cross-branch propagation. A subscriber receives three publications and
     * should only accept the ones that are closer than what it has previously received.
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

        // --- Send multiple publications ---
        // 1. A first publication from the publisher's default location. This should be received.
        System.out.println("Sending first publication from " + p2.getLocation());
        p2.send(new PublicationWithLocation(p2.getLocation()));

        // 2. A second, farther away publication. This should be filtered out by the leaf broker.
        Location fartherLocation = new Location(25, 10, 0); // Farther from s8 at (20, 5, 0)
        System.out.println("Sending second, farther publication from " + fartherLocation);
        p2.send(new PublicationWithLocation(fartherLocation));
        
        // 3. A third, closer publication. This should be an improvement and be delivered.
        Location closerLocation = new Location(19.5, 5.0, 0); // Closer to s8
        System.out.println("Sending third, closer publication from " + closerLocation);
        p2.send(new PublicationWithLocation(closerLocation));


        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber 's8' received: " + s8.getnPublications() + " publications. (Expected: 2)");

        boolean success = s8.getnPublications() == 2;

        if (success) {
            System.out.println("\n--- Validation Result ---");
            System.out.println("SUCCESS: The Comprehensive Scenario (Location) test passed. Correctly received 2 of 3 publications.");
        } else {
            System.out.println("\n--- Validation Result ---");
            System.out.println("FAILED: The Comprehensive Scenario (Location) test did not pass.");
        }

        return success;
    };

    /**
     * Validates that redundant subscriptions for the same location are filtered out
     * at the leaf broker, and that unique subscriptions from different branches are
     * correctly propagated to a common ancestor.
     */
    public static final Predicate<BrokerWithRegion> SUBSCRIPTION_FILTERING_SCENARIO = root -> {
        System.out.println("\n>>> SCENARIO: Testing Subscription Filtering for Redundant Locations. <<<");

        // --- Find required nodes ---
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class); // Under grandchild2
        SubscriberWithLocation s6 = findNodeByName(root, "sub6", SubscriberWithLocation.class); // Also under grandchild2
        SubscriberWithLocation s8 = findNodeByName(root, "sub8", SubscriberWithLocation.class); // Under grandchild4 (different branch)

        if (s2 == null || s6 == null || s8 == null) {
            System.err.println("Test failed: Could not find required nodes (s2, s6, s8).");
            return false;
        }

        Location sharedLocation = new Location(7, 3.5, 0);

        // --- Send Subscriptions ---
        // 1. s2 sends a subscription. Its parent, grandchild2, should propagate a proxy subscription.
        System.out.println("s2 sends subscription for location: " + sharedLocation);
        s2.send(new SubscriptionWithLocation(sharedLocation));

        // 2. s6 sends a redundant subscription for the same location. Its parent, grandchild2, should filter it.
        System.out.println("s6 sends a redundant subscription for the same location: " + sharedLocation);
        s6.send(new SubscriptionWithLocation(sharedLocation));

        // 3. s8 sends a unique subscription from a different branch. This should be propagated.
        System.out.println("s8 sends subscription for its unique location: " + s8.getLocation());
        s8.send(new SubscriptionWithLocation(s8.getLocation()));

        System.out.println("\n--- Final Check ---");
        // The root broker should have received two unique proxy subscriptions:
        // - One from the child2 branch (representing the de-duplicated interest in sharedLocation)
        // - One from the child3 branch (representing the interest from s8)
        int rootSubscriptionCount = root.getSubscriptionsTable().size();
        System.out.println("  - Root broker's subscription table size: " + rootSubscriptionCount + " (Expected: 2)");

        boolean success = (rootSubscriptionCount == 2);

        if (success) {
            System.out.println("\n--- Validation Result ---");
            System.out.println("SUCCESS: The Subscription Filtering (Location) test passed. Redundant subscription was correctly filtered and unique subscriptions were propagated.");
        } else {
            System.out.println("\n--- Validation Result ---");
            System.out.println("FAILED: The Subscription Filtering (Location) test did not pass.");
        }
        
        return success;
    };
    
    // Helper method for finding named nodes in the topology
    private static <T extends TreeNode> T findNodeByName(TreeNode root, String name, Class<T> type) {
        if (root == null || name == null) return null;
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