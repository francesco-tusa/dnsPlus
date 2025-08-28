package simulator.simulations.validation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.PublisherWithLocation;
import simulator.SubscriberWithLocation;
import simulator.SubscriptionWithLocation;
import simulator.TreeNode;
import simulator.regions.BrokerWithRegion;
import simulator.regions.Region;
import simulator.regions.SubscriptionWithRegion;

/**
 * A utility class containing static methods for common validation tests.
 * These tests can be passed to the ConfigurableValidationSimulation.
 */
public class ValidationTests {

    /**
     * Validation Test: Sanity check for the Fixed Topology.
     * Runs the original scenario from FixedTopologyValidationSimulation.
     */
    public static final Predicate<BrokerWithRegion> FIXED_TOPOLOGY_SANITY_CHECK = root -> {
        System.out.println("\n>>> Scenario: Sending Subscriptions <<<");
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s5 = findNodeByName(root, "sub5", SubscriberWithLocation.class);
        PublisherWithLocation p1 = findNodeByName(root, "pub1", PublisherWithLocation.class);
        PublisherWithLocation p2 = findNodeByName(root, "pub2", PublisherWithLocation.class);

        if (s2 == null || s5 == null || p1 == null || p2 == null) {
            System.err.println("Fixed Topology Test failed: Could not find all required nodes (sub2, sub5, pub1, pub2).");
            return false;
        }

        s2.send(new SubscriptionWithRegion(new Region(new Location(5, 2, 0), new Location(8, 5, 0))));
        s5.send(new SubscriptionWithRegion(new Region(new Location(2, 2, 0), new Location(4, 3, 0))));

        System.out.println("\n>>> Scenario: Sending Publications <<<");
        p1.send(new PublicationWithLocation(new Location(7, 3, 0))); // Should match s2
        p2.send(new PublicationWithLocation(new Location(19, 4, 0))); // Should not match anyone

        // Test passes if s2 received one publication and s5 received none.
        return s2.getnPublications() == 1 && s5.getnPublications() == 0;
    };

    /**
     * Validation Test: Sanity check for the Random Topology.
     * Runs the original scenario from RandomTopologyValidationSimulation.
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
        Region subReg = new Region(new Location(subLoc.getX() - 5, subLoc.getY() - 5, 0), new Location(subLoc.getX() + 5, subLoc.getY() + 5, 0));
        subscriber.send(new SubscriptionWithRegion(subReg));

        System.out.println("\n>>> Scenario: Publisher sends matching publication <<<");
        publisher.send(new PublicationWithLocation(subLoc));

        // Test passes if the subscriber received the publication.
        return subscriber.getnPublications() > 0;
    };


    // --- Helper methods for finding nodes ---

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