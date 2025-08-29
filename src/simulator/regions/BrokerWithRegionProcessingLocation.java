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
 * A broker that implements location-based routing. It uses a cache of key points
 * to determine whether to propagate publications downwards.
 */
public class BrokerWithRegionProcessingLocation extends BrokerWithRegion {

    private final Map<Location, SimulationPublication> bestPublicationCache = new HashMap<>();

    public BrokerWithRegionProcessingLocation(String name) {
        super(name);
    }

    public BrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing publication from " + p.getSource().getName());

        // 1. Always propagate the publication upwards to the parent.
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null && p.getSource() != parentBroker) {
            System.out.println(getName() + ": Propagating publication upwards to " + parentBroker.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parentBroker.processPublication(forwardedCopy);
        }

        // 2. Process the publication for downward propagation.
        System.out.println(getName() + ": Processing publication for downward propagation.");
        if (p instanceof PublicationWithLocation pub) {
            processPublicationDownward(pub);
        }

        return null;
    }

    /**
     * This is the core logic for location-based routing. It checks if a new publication
     * is an "improvement" for any of the key points in this broker's region.
     */
    protected void processPublicationDownward(PublicationWithLocation pub) {
        if (getRegion() == null) {
            System.err.println(getName() + ": Cannot process downward propagation, region is not set.");
            return;
        }

        boolean isImprovement = false;
        // Check the new publication against the cache for each key point.
        for (Location keyPoint : getRegion().getKeyPoints()) {
            SimulationPublication cachedPub = bestPublicationCache.get(keyPoint);

            if (cachedPub == null || 
                distanceSquared(pub.getLocation(), keyPoint) < distanceSquared(((PublicationWithLocation) cachedPub).getLocation(), keyPoint)) 
            {
                System.out.println(getName() + ": New publication is closer to key point " + keyPoint + ". Caching and forwarding.");
                bestPublicationCache.put(keyPoint, pub);
                isImprovement = true;
            }
        }

        if (isImprovement) {
            // If it's an improvement for at least one key point, forward it to all children
            // except the one it came from.
            for (TreeNode child : getChildren()) {
                if (child instanceof BrokerWithRegion childBroker && child != pub.getSource()) {
                    System.out.println(getName() + ": Forwarding publication down to " + child.getName());
                    SimulationPublication forwardedCopy = pub.getPublication();
                    forwardedCopy.setSource(this);
                    childBroker.processPublication(forwardedCopy);
                }
            }
        } else {
            System.out.println(getName() + ": Publication is not an improvement for any key point. Stopping downward propagation.");
        }
    }

    /**
     * Calculates the squared Euclidean distance between two locations.
     * Squared distance is used to avoid expensive square root operations for simple comparisons.
     */
    protected double distanceSquared(Location l1, Location l2) {
        double dx = l1.getX() - l2.getX();
        double dy = l1.getY() - l2.getY();
        double dz = l1.getZ() - l2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
