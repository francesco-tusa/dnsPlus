package simulator.simulations.validation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;
import simulator.regions.BrokerWithRegion;
import simulator.SubscriptionWithLocation;

/**
 * A utility class containing static validation tests specifically for the Fixed Topology
 * when using LOCATION-based brokers.
 */
public class ManualTopologyLocationValidationTests {

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