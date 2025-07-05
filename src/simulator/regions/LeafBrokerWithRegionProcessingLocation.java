package simulator.regions;

import java.util.HashMap;
import java.util.Map;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

/**
 * The leaf-level broker for the location-based upward propagation routing strategy.
 * This broker is directly connected to subscribers and uses an internal cache
 * to prevent sending redundant publications.
 */
public class LeafBrokerWithRegionProcessingLocation extends BrokerWithRegionProcessingLocation {

    private final Map<SubscriberWithLocation, PublicationWithLocation> subscriberCache = new HashMap<>();

    public LeafBrokerWithRegionProcessingLocation(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    /**
     * Overrides the downward processing logic for leaf brokers. It now uses its
     * internal caching mechanism to determine if a publication should be forwarded.
     * @param p The publication received from the parent.
     */
    @Override
    protected void processPublicationDownward(SimulationPublication p) {
        System.out.println(getName() + ": Processing publication for delivery to subscribers.");
        PublicationWithLocation newPublication = (PublicationWithLocation) p;

        for (TreeNode child : getChildren()) {
            if (child instanceof SubscriberWithLocation subscriber) {
                PublicationWithLocation cachedPub = subscriberCache.get(subscriber);

                if (cachedPub == null || 
                    distanceSquared(newPublication.getLocation(), subscriber.getLocation()) < distanceSquared(cachedPub.getLocation(), subscriber.getLocation())) {
                    
                    forwardPublicationToNode(newPublication, subscriber);
                    subscriberCache.put(subscriber, newPublication);
                }
            }
        }
    }

    @Override
    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        if (next instanceof SubscriberWithLocation subscriber) {
            System.out.println(getName() + ": Delivering publication to subscriber " + subscriber.getName());
            subscriber.receive(p);
        } else {
            System.err.println(getName() + ": Topology error, expected a subscriber but got " + next.getClass().getSimpleName());
        }
    }
}
