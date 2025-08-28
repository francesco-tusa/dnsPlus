package simulator.regions;

import java.util.HashMap;
import java.util.Map;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

/**
 * The leaf-level broker for the location-based upward propagation routing strategy.
 * This broker is directly connected to subscribers and now correctly validates
 * publications against subscriptions before delivery.
 */
public class LeafBrokerWithRegionProcessingLocation extends BrokerWithRegionProcessingLocation {

    public LeafBrokerWithRegionProcessingLocation(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    /**
     * Overrides the default addChild to ensure the region is updated when a subscriber is added.
     */
    @Override
    public void addChild(TreeNode child) {
        super.addChild(child);
        System.out.println(getName() + ": added new " + child.getName());
        // When a subscriber is added, update the leaf's region and propagate the change upwards.
        updateRegion(child);
    }

    /**
     * Overrides the downward processing logic for leaf brokers. It now checks
     * the publication against each subscriber's specific subscription region.
     * @param p The publication received from the parent.
     */
    @Override
    protected void processPublicationDownward(SimulationPublication p) {
        System.out.println(getName() + ": Processing publication for delivery to subscribers.");
        PublicationWithLocation newPublication = (PublicationWithLocation) p;

        for (TreeNode child : getChildren()) {
            if (child instanceof SubscriberWithLocation subscriber) {
                // Get the subscription for this specific subscriber from the broker's table.
                SimulationSubscription subscription = getSubscriptionsTable().get(subscriber);

                if (subscription instanceof SubscriptionWithRegion subRegion) {
                    // This is the crucial check: Is the publication's location inside the subscriber's region?
                    if (subRegion.getRegion().contains(newPublication.getLocation())) {
                        System.out.println(getName() + ": Publication matches subscription for " + subscriber.getName() + ". Delivering.");
                        subscriber.receive(newPublication);
                    } else {
                        System.out.println(getName() + ": Publication does NOT match subscription for " + subscriber.getName() + ". Filtering.");
                    }
                }
            }
        }
    }
}