package simulator.regions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationBroker;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

/**
 * A broker that implements a location-based subscription and publication propagation strategy.
 */
public class BrokerWithRegionProcessingLocation extends BrokerWithRegion {

    // A cache to store the best known publication for key points within this broker's region.
    private final Map<Location, PublicationWithLocation> publicationCache = new HashMap<>();
    private List<Location> keyPoints;

    public BrokerWithRegionProcessingLocation(String name) {
        super(name);
    }

    public BrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public void processSubscription(SimulationSubscription s) {
        System.out.println(getName() + ": processing subscription " + s + " from " + s.getSource().getName());
        addSubscription(s);
        
        SimulationBroker parentBroker = getParentBroker();
        if (parentBroker != null) {
            if (parentBroker.getSubscriptionsTable().containsKey(this)) {
                System.out.println(parentBroker.getName() + " already has a subscription from " + this.getName() + ". Stopping upward propagation.");
            } else {
                System.out.println("Forwarding subscription from " + this.getName() + " to parent " + parentBroker.getName());
                s.setSource(this);
                parentBroker.processSubscription(s);
            }
        } else {
            System.out.println(getName() + " is the root or has no parent. Upward propagation stops.");
        }
    }

    /**
     * Processes an incoming publication by propagating it up and then selectively down.
     * @param p The publication to process.
     */
    @Override
    public void processPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing publication from " + p.getSource().getName());

        // --- Upward Propagation ---
        // OPTIMIZATION NOTE: Propagating every single publication to the root is inefficient.
        // A potential optimization is to only propagate publications that are closer to the
        // region's corners and midpoints than previously seen ones. However, this heuristic
        // could fail in complex scenarios with many overlapping sub-regions, potentially
        // missing the optimal announcement for a sub-region.
        SimulationBroker parent = getParentBroker();
        if (parent != null) {
            System.out.println(getName() + ": Propagating publication upwards to " + parent.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parent.processPublication(forwardedCopy);
        }

        // --- Downward Propagation ---
        processPublicationDownward(p);
    }

    /**
     * Handles the downward propagation and caching logic for non-leaf brokers.
     * @param p The publication to process.
     */
    protected void processPublicationDownward(SimulationPublication p) {
        System.out.println(getName() + ": Processing publication for downward propagation.");
        PublicationWithLocation newPublication = (PublicationWithLocation) p;
        boolean shouldForward = false;

        // 1a) Decide if this announcement is better than any we've seen.
        if (keyPoints == null) {
            this.keyPoints = calculateKeyPoints();
        }

        for (Location point : keyPoints) {
            PublicationWithLocation cachedPub = publicationCache.get(point);
            if (cachedPub == null || distanceSquared(newPublication.getLocation(), point) < distanceSquared(cachedPub.getLocation(), point)) {
                // This new publication is better for at least one key point.
                System.out.println(getName() + ": New publication is closer to key point " + point + ". Caching and forwarding.");
                publicationCache.put(point, newPublication); // 1b) Cache publication
                shouldForward = true;
            }
        }

        if (shouldForward) {
            for (TreeNode child : getChildren()) {
                if (child instanceof BrokerWithRegionProcessingLocation && child != p.getSource()) {
                    System.out.println(getName() + ": Forwarding publication down to " + child.getName());
                    SimulationPublication forwardedCopy = p.getPublication();
                    forwardedCopy.setSource(this);
                    // A non-leaf broker calls processPublication on its children.
                    ((BrokerWithRegionProcessingLocation) child).processPublication(forwardedCopy);
                }
            }
        } else {
            System.out.println(getName() + ": Publication is not an improvement for any key point. Stopping downward propagation.");
        }
    }

    /**
     * Calculates the 9 key points (vertices, edge midpoints, centroid) of this broker's region.
     * @return A list of key point locations.
     */
    private List<Location> calculateKeyPoints() {
        List<Location> points = new ArrayList<>();
        Region region = getRegion();
        if (region == null || region.getBottomLeft() == null) return points;

        double minX = region.getBottomLeft().getX();
        double minY = region.getBottomLeft().getY();
        double maxX = region.getTopRight().getX();
        double maxY = region.getTopRight().getY();
        double midX = minX + (maxX - minX) / 2;
        double midY = minY + (maxY - minY) / 2;

        // 4 Vertices
        points.add(new Location(minX, minY, 0));
        points.add(new Location(maxX, minY, 0));
        points.add(new Location(minX, maxY, 0));
        points.add(new Location(maxX, maxY, 0));
        // 4 Edge Midpoints
        points.add(new Location(midX, minY, 0));
        points.add(new Location(midX, maxY, 0));
        points.add(new Location(minX, midY, 0));
        points.add(new Location(maxX, midY, 0));
        // 1 Centroid
        points.add(new Location(midX, midY, 0));
        
        return points;
    }

    protected double distanceSquared(Location l1, Location l2) {
        double dx = l1.getX() - l2.getX();
        double dy = l1.getY() - l2.getY();
        return (dx * dx) + (dy * dy);
    }
    
    // --- Other Methods ---

    @Override
    public void addSubscription(SimulationSubscription s) {
        getSubscriptionsTable().put(s.getSource(), s.getTableEntry());
    }

    @Override
    public boolean regionsOrLocationsMatch(SimulationSubscription existingSubscription, SimulationSubscription newSubscription) {
        return false; // Not used in this routing model
    }

    @Override
    public void processPublicationLocation(SimulationPublication p, TreeNode next) {
        // Not directly called; logic is in processPublicationDownward
    }

    @Override
    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        // Not directly called; logic is in processPublicationDownward
    }
         
    @Override
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        // Intentionally empty for this upward-only subscription model
    }

    @Override
    protected void updateSubscriptions(SimulationSubscription existingSubscription, SimulationSubscription newSubscription) {
        // Intentionally empty
    }
}
