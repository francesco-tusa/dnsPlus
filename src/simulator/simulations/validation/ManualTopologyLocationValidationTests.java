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
 * A utility class containing static validation tests specifically for the Fixed Topology
 * when using LOCATION-based brokers.
 */
public class ManualTopologyLocationValidationTests {

    /**
     * A basic sanity check. It verifies that a simple, direct publication is
     * correctly received by one subscriber and correctly filtered for another.
     */
    public static final Predicate<BrokerWithRegion> SANITY_CHECK = root -> {
        System.out.println("\n>>> SCENARIO: Testing simple delivery for Location-based brokers. <<<");
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s5 = findNodeByName(root, "sub5", SubscriberWithLocation.class);
        PublisherWithLocation p1 = findNodeByName(root, "pub1", PublisherWithLocation.class);
        
        if (s2 == null || s5 == null || p1 == null) {
            System.err.println("Test failed: Could not find required nodes (sub2, sub5, pub1).");
            return false;
        }

        s2.send(new SubscriptionWithRegion(new Region(new Location(5, 2, 0), new Location(8, 5, 0))));
        s5.send(new SubscriptionWithRegion(new Region(new Location(2, 2, 0), new Location(4, 3, 0))));
        p1.send(new PublicationWithLocation(new Location(7, 3, 0))); // Should only match s2

        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber 's2' received: " + s2.getnPublications() + " publications. (Expected: 1)");
        System.out.println("  - Subscriber 's5' received: " + s5.getnPublications() + " publications. (Expected: 0)");

        return s2.getnPublications() == 1 && s5.getnPublications() == 0;
    };
    
    // --- Helper method for finding named nodes in the topology ---
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
