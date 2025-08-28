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
import simulator.TreeNode;

/**
 * A broker that implements a location-based subscription and publication propagation strategy.
 */
public class BrokerWithRegionProcessingLocation extends BrokerWithRegion {

    private final Map<Location, PublicationWithLocation> publicationCache = new HashMap<>();
    private List<Location> keyPoints;

    public BrokerWithRegionProcessingLocation(String name) {
        super(name);
    }

    public BrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing publication from " + p.getSource().getName());

        // Upward Propagation
        SimulationBroker parent = getParentBroker();
        if (parent != null) {
            System.out.println(getName() + ": Propagating publication upwards to " + parent.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parent.processPublication(forwardedCopy);
        }

        // Downward Propagation
        processPublicationDownward(p);
        return null; // This method is for routing, not returning a single match.
    }

    /**
     * Handles the downward propagation and caching logic for non-leaf brokers.
     * Made protected so leaf brokers can override it.
     * @param p The publication to process.
     */
    protected void processPublicationDownward(SimulationPublication p) {
        System.out.println(getName() + ": Processing publication for downward propagation.");
        PublicationWithLocation newPublication = (PublicationWithLocation) p;
        boolean shouldForward = false;

        if (keyPoints == null) {
            this.keyPoints = calculateKeyPoints();
        }

        for (Location point : keyPoints) {
            PublicationWithLocation cachedPub = publicationCache.get(point);
            if (cachedPub == null || distanceSquared(newPublication.getLocation(), point) < distanceSquared(cachedPub.getLocation(), point)) {
                System.out.println(getName() + ": New publication is closer to key point " + point + ". Caching and forwarding.");
                publicationCache.put(point, newPublication);
                shouldForward = true;
            }
        }

        if (shouldForward) {
            for (TreeNode child : getChildren()) {
                if (child instanceof BrokerWithRegionProcessingLocation && child != p.getSource()) {
                    System.out.println(getName() + ": Forwarding publication down to " + child.getName());
                    SimulationPublication forwardedCopy = p.getPublication();
                    forwardedCopy.setSource(this);
                    ((BrokerWithRegionProcessingLocation) child).processPublication(forwardedCopy);
                }
            }
        } else {
            System.out.println(getName() + ": Publication is not an improvement for any key point. Stopping downward propagation.");
        }
    }

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

        points.add(new Location(minX, minY, 0));
        points.add(new Location(maxX, minY, 0));
        points.add(new Location(minX, maxY, 0));
        points.add(new Location(maxX, maxY, 0));
        points.add(new Location(midX, minY, 0));
        points.add(new Location(midX, maxY, 0));
        points.add(new Location(minX, midY, 0));
        points.add(new Location(maxX, midY, 0));
        points.add(new Location(midX, midY, 0));
        
        return points;
    }

    /**
     * Calculates the squared distance between two locations.
     * Made protected so subclasses can access it.
     */
    protected double distanceSquared(Location l1, Location l2) {
        double dx = l1.getX() - l2.getX();
        double dy = l1.getY() - l2.getY();
        return (dx * dx) + (dy * dy);
    }
}