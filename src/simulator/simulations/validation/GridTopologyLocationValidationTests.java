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
 * A utility class containing static validation tests specifically for Grid Topologies
 * using location-based processing brokers.
 */
public class GridTopologyLocationValidationTests {

    /**
     * A validation test for a grid topology that checks if a publication
     * sent from one corner of the grid is correctly received by a subscriber
     * in the opposite corner. This validates the propagation of messages
     * across the entire broker hierarchy using location-based routing.
     */
    public static final Predicate<BrokerWithRegion> GRID_CROSS_CORNER_PROPAGATION = root -> {
        System.out.println("\n>>> SCENARIO: Running Grid Topology Cross-Corner Propagation Test (Location). <<<");

        // --- Find corner nodes ---
        // This test assumes a 3x3 grid, as configured in the main runner.
        SubscriberWithLocation subscriber = findNodeByName(root, "sub-0-0-0", SubscriberWithLocation.class);
        PublisherWithLocation publisher = findNodeByName(root, "pub-2-2-0", PublisherWithLocation.class);

        if (subscriber == null || publisher == null) {
            System.err.println("Test failed: Could not find required corner nodes (sub-0-0-0, pub-2-2-0). This test requires a 3x3 grid.");
            return false;
        }

        // --- Send Subscription ---
        // Subscriber at (0,0) subscribes with its location.
        subscriber.send(new SubscriptionWithLocation(subscriber.getLocation()));

        // --- Send Publication ---
        // Publisher sends from its location.
        publisher.send(new PublicationWithLocation(publisher.getLocation()));

        // --- Verification ---
        // The core of the location-based algorithm is "best effort" delivery of the *closest*
        // publication. A single publication should always be considered the best one so far.
        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber 'sub-0-0-0' received: " + subscriber.getnPublications() + " publications. (Expected: 1)");

        boolean success = subscriber.getnPublications() == 1;

        if (success) {
            System.out.println("\n--- Validation Result ---");
            System.out.println("SUCCESS: The Grid Cross-Corner Propagation (Location) test passed.");
        } else {
            System.out.println("\n--- Validation Result ---");
            System.out.println("FAILED: The Grid Cross-Corner Propagation (Location) test did not pass.");
        }

        return success;
    };

    /**
     * Helper method to find a node by its name within the topology tree.
     */
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