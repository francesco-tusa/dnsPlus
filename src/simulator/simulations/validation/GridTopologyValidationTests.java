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

/**
 * A utility class containing static validation tests specifically for Grid Topologies.
 */
public class GridTopologyValidationTests {

    /**
     * A basic sanity check for grid topologies. It places a publisher and subscriber
     * at the same location on a leaf broker and verifies the publication is received.
     */
    public static final Predicate<BrokerWithRegion> GUARANTEED_MATCH = root -> {
        System.out.println("\n>>> SCENARIO: Testing guaranteed match on a grid topology leaf. <<<");
        BrokerWithRegion leaf = findFirstLeafBroker(root);
        if (leaf == null) {
            System.err.println("Test failed: No leaf broker found in the topology.");
            return false;
        }
        
        Location testLocation = leaf.getRegion().getBottomLeft();
        if (testLocation == null) {
            System.err.println("Test failed: Leaf broker's region has no valid location.");
            return false;
        }

        SubscriberWithLocation subscriber = new SubscriberWithLocation("Sub-Test", testLocation);
        leaf.addChild(subscriber);
        subscriber.send(new SubscriptionWithLocation(subscriber.getLocation()));

        PublisherWithLocation publisher = new PublisherWithLocation("Pub-Test", testLocation);
        leaf.addChild(publisher);
        publisher.send(new PublicationWithLocation(publisher.getLocation()));

        System.out.println("\n--- Final Check ---");
        System.out.println("  - Subscriber 'Sub-Test' received: " + subscriber.getnPublications() + " publications. (Expected: 1)");

        return subscriber.getnPublications() == 1;
    };

    // --- Helper method to find the first leaf broker in a topology ---
    private static BrokerWithRegion findFirstLeafBroker(TreeNode root) {
        if (root == null) return null;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            if (current instanceof BrokerWithRegion) {
                boolean hasBrokerChild = false;
                for (TreeNode child : current.getChildren()) {
                    if (child instanceof BrokerWithRegion) {
                        hasBrokerChild = true;
                        break;
                    }
                }
                if (!hasBrokerChild) {
                    return (BrokerWithRegion) current;
                }
            }
            if (current.getChildren() != null) {
                queue.addAll(current.getChildren());
            }
        }
        return null;
    }
}
