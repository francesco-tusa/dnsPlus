package simulator.simulations.functional;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;

import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

/**
 * A utility class containing static validation tests specifically for Grid Topologies
 * using region-based processing brokers.
 */
public class GridTopologyRegionFunctionalTests {

    /**
     * A validation test for a grid topology that checks if a publication
     * sent from one corner of the grid is correctly received by a subscriber
     * in the opposite corner. This validates the propagation of messages
     * across the entire broker hierarchy.
     */
    public static final Predicate<BrokerWithRegion> GRID_CROSS_CORNER_PROPAGATION = root -> {
        System.out.println("\n>>> SCENARIO: Running Grid Topology Cross-Corner Propagation Test. <<<");

        // --- Find corner nodes ---
        // This test assumes a 3x3 grid, as configured in the main runner.
        SubscriberWithLocation subscriber = findNodeByName(root, "sub-0-0-0", SubscriberWithLocation.class);
        PublisherWithLocation publisher = findNodeByName(root, "pub-2-2-0", PublisherWithLocation.class);

        if (subscriber == null || publisher == null) {
            System.err.println("Test failed: Could not find required corner nodes (sub-0-0-0, pub-2-2-0). This test requires a 3x3 grid.");
            return false;
        }

        // --- Send Subscription ---
        // Subscriber at (0,0) subscribes to a region that specifically includes the publisher's location.
        Location publisherLocation = publisher.getLocation();
        subscriber.send(new SubscriptionWithRegion(new Region(publisherLocation, publisherLocation)));

        // --- Send Publication ---
        publisher.send(new PublicationWithLocation(publisher.getLocation()));

        // --- Verification ---
        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber 'sub-0-0-0' received: " + subscriber.getnPublications() + " publications. (Expected: 1)");

        boolean success = subscriber.getnPublications() == 1;

        if (success) {
            System.out.println("\n--- Validation Result ---");
            System.out.println("SUCCESS: The Grid Cross-Corner Propagation test passed.");
        } else {
            System.out.println("\n--- Validation Result ---");
            System.out.println("FAILED: The Grid Cross-Corner Propagation test did not pass.");
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
