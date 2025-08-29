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

    public static final Predicate<BrokerWithRegion> SANITY_CHECK_NO_OVERLAP = root -> {
        System.out.println("\n>>> SCENARIO: Testing simple, non-overlapping subscription delivery. <<<");
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
    
    public static final Predicate<BrokerWithRegion> CROSS_BRANCH_PROPAGATION = root -> {
        System.out.println("\n>>> SCENARIO: Testing downward propagation to an overlapping branch. <<<");
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        BrokerWithRegion grandchild1 = findNodeByName(root, "grandchild1", BrokerWithRegion.class);

        if (s2 == null || grandchild1 == null) {
            System.err.println("Test failed: Could not find required nodes (sub2, grandchild1).");
            return false;
        }

        Region overlappingRegion = new Region(new Location(4, 2, 0), new Location(7, 4, 0));
        s2.send(new SubscriptionWithRegion(overlappingRegion));

        PublisherWithLocation overlapPublisher = new PublisherWithLocation("pub-overlap-test", new Location(4, 2.5, 0));
        grandchild1.addChild(overlapPublisher);
        overlapPublisher.send(new PublicationWithLocation(overlapPublisher.getLocation()));

        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber 's2' received: " + s2.getnPublications() + " publications. (Expected: 1)");
        
        return s2.getnPublications() == 1;
    };

    public static final Predicate<BrokerWithRegion> COMBINED_COMPLEX_SCENARIO = root -> {
        System.out.println("\n>>> SCENARIO: Testing both overlapping and non-overlapping propagation. <<<");
        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s5 = findNodeByName(root, "sub5", SubscriberWithLocation.class);
        BrokerWithRegion grandchild1 = findNodeByName(root, "grandchild1", BrokerWithRegion.class);

        if (s2 == null || s5 == null || grandchild1 == null) {
            System.err.println("Complex Test failed: Could not find all required nodes.");
            return false;
        }

        // Action 1: s2 sends a subscription that overlaps with grandchild1's region.
        s2.send(new SubscriptionWithRegion(new Region(new Location(4, 2, 0), new Location(7, 4, 0))));
        // Action 2: s5 sends a subscription that does NOT overlap with the publication.
        s5.send(new SubscriptionWithRegion(new Region(new Location(2, 2, 0), new Location(3, 3, 0))));

        // Action 3: A publisher under grandchild1 sends a publication that SHOULD match s2.
        PublisherWithLocation overlapPublisher = new PublisherWithLocation("pub-overlap-test", new Location(4, 2.5, 0));
        grandchild1.addChild(overlapPublisher);
        overlapPublisher.send(new PublicationWithLocation(overlapPublisher.getLocation()));

        // --- Verification ---
        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber 's2' received: " + s2.getnPublications() + " publications. (Expected: 1)");
        System.out.println("  - Subscriber 's5' received: " + s5.getnPublications() + " publications. (Expected: 0)");

        return s2.getnPublications() == 1 && s5.getnPublications() == 0;
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
