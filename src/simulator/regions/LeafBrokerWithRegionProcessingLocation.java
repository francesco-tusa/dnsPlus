package simulator.regions;

import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

/**
 * A leaf broker that uses location-based routing. It receives publications from
 * its parent and delivers them to its directly connected subscribers.
 */
public class LeafBrokerWithRegionProcessingLocation extends BrokerWithRegionProcessingLocation {

    public LeafBrokerWithRegionProcessingLocation(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }
    
    /**
     * Correctly overrides the parent method. The logic for a leaf broker is to
     * deliver the publication to its subscribers, not to propagate it further down.
     */
    @Override
    protected void processPublicationDownward(PublicationWithLocation pub) {
        System.out.println(getName() + ": Processing publication for delivery to subscribers.");
        for (TreeNode child : getChildren()) {
            if (child instanceof SubscriberWithLocation subscriber) {
                // This is the check that was missing before.
                // It ensures the publication location is within the subscriber's region of interest.
                SimulationSubscription sub = getSubscriptionsTable().get(subscriber);
                if (sub instanceof SubscriptionWithRegion subRegion) {
                    if (subRegion.getRegion().contains(pub.getLocation())) {
                        System.out.println(getName() + ": Delivering publication to subscriber " + subscriber.getName());
                        subscriber.receive(pub);
                    } else {
                        System.out.println(getName() + ": Publication does NOT match subscription for " + subscriber.getName() + ". Filtering.");
                    }
                }
            }
        }
    }
}
