package simulator.simulations.validation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;
import simulator.PublicationWithLocation;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.SubscriptionWithLocation;
import simulator.TreeNode;
import simulator.regions.BrokerWithRegion;

/**
 * A utility class containing static validation tests specifically for Random Topologies
 * using location-based processing brokers.
 */
public class RandomTopologyLocationValidationTests {

    /**
     * A basic sanity check for a randomly generated topology. It ensures that if a
     * subscriber sends a subscription, a publisher's publication will be successfully
     * propagated through the hierarchy and delivered.
     */
    public static final Predicate<BrokerWithRegion> SANITY_CHECK = root -> {
        System.out.println("\n>>> SCENARIO: Running Random Topology Sanity Check (Location). <<<");

        SubscriberWithLocation subscriber = findFirstNodeOfType(root, SubscriberWithLocation.class);
        PublisherWithLocation publisher = findFirstNodeOfType(root, PublisherWithLocation.class);

        if (subscriber == null || publisher == null) {
            System.err.println("Random Topology Test failed: Could not find at least one subscriber and one publisher.");
            return false;
        }

        // --- Send Subscription ---
        subscriber.send(new SubscriptionWithLocation(subscriber.getLocation()));

        // --- Send Publication ---
        publisher.send(new PublicationWithLocation(publisher.getLocation()));

        // --- Verification ---
        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber '" + subscriber.getName() + "' received: " + subscriber.getnPublications() + " publications. (Expected: >0)");
        
        boolean success = subscriber.getnPublications() > 0;

        if (success) {
            System.out.println("\n--- Validation Result ---");
            System.out.println("SUCCESS: The Random Topology Sanity Check (Location) test passed.");
        } else {
            System.out.println("\n--- Validation Result ---");
            System.out.println("FAILED: The Random Topology Sanity Check (Location) test did not pass.");
        }
        
        return success;
    };

    // Helper method for finding the first node of a specific type
    private static <T extends TreeNode> T findFirstNodeOfType(TreeNode root, Class<T> type) {
        if (root == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            if (current.getChildren() != null) {
                queue.addAll(current.getChildren());
            }
        }
        return null;
    }
}