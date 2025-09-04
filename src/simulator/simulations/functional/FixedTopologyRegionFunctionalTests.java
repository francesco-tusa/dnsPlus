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

public class FixedTopologyRegionFunctionalTests {

    public static final Predicate<BrokerWithRegion> COMPREHENSIVE_SCENARIO = root -> {
        System.out.println("\n>>> SCENARIO: Running Comprehensive Cross-Branch and Local Propagation Test. <<<");

        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s8 = findNodeByName(root, "sub8", SubscriberWithLocation.class);
        SubscriberWithLocation s5 = findNodeByName(root, "sub5", SubscriberWithLocation.class);
        PublisherWithLocation p1 = findNodeByName(root, "pub1", PublisherWithLocation.class);
        PublisherWithLocation p2 = findNodeByName(root, "pub2", PublisherWithLocation.class);

        if (s2 == null || s8 == null || s5 == null || p1 == null || p2 == null) {
            System.err.println("Test failed: Could not find all required nodes for the comprehensive test.");
            return false;
        }

        s2.send(new SubscriptionWithRegion(new Region(new Location(16, 3, 0), new Location(19, 5, 0))));
        s8.send(new SubscriptionWithRegion(new Region(new Location(17, 3, 0), new Location(19, 5, 0))));
        s5.send(new SubscriptionWithRegion(new Region(new Location(6, 6, 0), new Location(8, 8, 0))));

        p2.send(new PublicationWithLocation(p2.getLocation()));
        p1.send(new PublicationWithLocation(p1.getLocation()));

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

    public static final Predicate<BrokerWithRegion> SUBSCRIPTION_COVERING_SCENARIO = root -> {
        System.out.println("\n>>> SCENARIO: Running Subscription Covering Test. <<<");

        SubscriberWithLocation s2 = findNodeByName(root, "sub2", SubscriberWithLocation.class);
        SubscriberWithLocation s3 = findNodeByName(root, "sub3", SubscriberWithLocation.class);

        if (s2 == null || s3 == null) {
            System.err.println("Test failed: Could not find required subscribers (s2, s3).");
            return false;
        }

        System.out.println("s2 (under grandchild2) sends a large subscription for region [0,0] to [20,20].");
        s2.send(new SubscriptionWithRegion(new Region(new Location(0, 0, 0), new Location(20, 20, 0))));

        System.out.println("s3 (under grandchild3) sends a small, covered subscription for region [5,5] to [10,10].");
        s3.send(new SubscriptionWithRegion(new Region(new Location(5, 5, 0), new Location(10, 10, 0))));

        System.out.println("\n--- Final Check ---");
        int rootSubscriptionCount = root.getSubscriptionsTable().size();
        System.out.println("  - Root broker's subscription table size: " + rootSubscriptionCount + " (Expected: 1)");

        boolean success = (rootSubscriptionCount == 1);

        if (success) {
            System.out.println("\n--- Validation Result ---");
            System.out.println("SUCCESS: The Subscription Covering test passed. Redundant subscription was correctly filtered before reaching the root.");
        } else {
            System.out.println("\n--- Validation Result ---");
            System.out.println("FAILED: The Subscription Covering test did not pass.");
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