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
 * A utility class containing static validation tests specifically for Random Topologies.
 */
public class RandomTopologyRegionValidationTests {

    /**
     * A basic sanity check for a randomly generated topology. It ensures that if a
     * subscriber subscribes to a region, a publisher publishing within that region
     * will successfully deliver a notification.
     */
    public static final Predicate<BrokerWithRegion> RANDOM_TOPOLOGY_SANITY_CHECK = root -> {
        SubscriberWithLocation subscriber = findFirstNodeOfType(root, SubscriberWithLocation.class);
        PublisherWithLocation publisher = findFirstNodeOfType(root, PublisherWithLocation.class);

        if (subscriber == null || publisher == null) {
            System.err.println("Random Topology Test failed: Could not find at least one subscriber and one publisher.");
            return false;
        }

        System.out.println("\n>>> Scenario: Subscriber sends subscription <<<");
        Location subLoc = subscriber.getLocation();
        
        // ** THE FIX **
        // Create a 3D subscription region that covers the full Z-axis (0 to 100),
        // ensuring it can contain the 3D publication location.
        Region subReg = new Region(
            new Location(subLoc.getX() - 5, subLoc.getY() - 5, 0), 
            new Location(subLoc.getX() + 5, subLoc.getY() + 5, 100)
        );
        subscriber.send(new SubscriptionWithRegion(subReg));

        System.out.println("\n>>> Scenario: Publisher sends matching publication <<<");
        System.out.println(publisher.getName() + ": sending publication from location " + subLoc + " (guaranteed match)");
        publisher.send(new PublicationWithLocation(subLoc));

        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber '" + subscriber.getName() + "' received: " + subscriber.getnPublications() + " publications. (Expected: >0)");
        
        return subscriber.getnPublications() > 0;
    };

    // --- Helper method for finding the first node of a specific type ---
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