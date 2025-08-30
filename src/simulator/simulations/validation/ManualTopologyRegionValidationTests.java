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
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

/**
 * A utility class containing static validation tests specifically for the
 * manually created Fixed Topology when using REGION-based brokers.
 */
public class ManualTopologyRegionValidationTests {

    /**
     * A comprehensive test case that validates multiple, complex routing scenarios:
     * 1.  A subscriber ('s2') receiving a publication from a publisher ('pub2') on a different branch.
     * 2.  A subscriber ('s8') receiving a publication from a local publisher ('pub2').
     * 3.  A subscriber ('s5') receiving a publication from a local publisher ('pub1') that should NOT be delivered to other subscribers.
     * This test ensures that cross-branch propagation works correctly and that there are no redundant deliveries (fan-out).
     */
    public static final Predicate<BrokerWithRegion> COMPREHENSIVE_SCENARIO = root -> {
        System.out.println("\n>>> SCENARIO: Running Comprehensive Cross-Branch and Local Propagation Test. <<<");

        // --- Find all required nodes ---
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s8 = findNodeByName(root, "sub8", SubscriberWithLocation.class);
        SubscriberWithLocation s5 = findNodeByName(root, "sub5", SubscriberWithLocation.class);
        PublisherWithLocation p1 = findNodeByName(root, "pub1", PublisherWithLocation.class);
        PublisherWithLocation p2 = findNodeByName(root, "pub2", PublisherWithLocation.class);

        if (s2 == null || s8 == null || s5 == null || p1 == null || p2 == null) {
            System.err.println("Test failed: Could not find all required nodes for the comprehensive test.");
            return false;
        }

        // --- Send Subscriptions ---
        // s2 (under child2) subscribes to a region that will match pub2.
        s2.send(new SubscriptionWithRegion(new Region(new Location(16, 3, 0), new Location(19, 5, 0))));
        // s8 (under child3) subscribes to a region that will also match pub2.
        s8.send(new SubscriptionWithRegion(new Region(new Location(17, 3, 0), new Location(19, 5, 0))));
        // s5 (under child1) subscribes to a region that will only match pub1.
        s5.send(new SubscriptionWithRegion(new Region(new Location(6, 6, 0), new Location(8, 8, 0))));

        // --- Send Publications ---
        // This publication from pub2 (location 18,4,0) should be received by s2 and s8 exactly once.
        p2.send(new PublicationWithLocation(p2.getLocation()));
        // This publication from pub1 (location 7,7,0) should be received by s5 exactly once.
        p1.send(new PublicationWithLocation(p1.getLocation()));


        // --- Verification ---
        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber 's2' received: " + s2.getnPublications() + " publications. (Expected: 1)");
        System.out.println("  - Subscriber 's8' received: " + s8.getnPublications() + " publications. (Expected: 1)");
        System.out.println("  - Subscriber 's5' received: " + s5.getnPublications() + " publications. (Expected: 1)");

        boolean success = s2.getnPublications() == 1 && s8.getnPublications() == 1 && s5.getnPublications() == 1;

        if (success) {
            System.out.println("\n--- Validation Result ---");
            System.out.println("SUCCESS: The Comprehensive Scenario test passed.");
        } else {
            System.out.println("\n--- Validation Result ---");
            System.out.println("FAILED: The Comprehensive Scenario test did not pass.");
        }

        return success;
    };

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