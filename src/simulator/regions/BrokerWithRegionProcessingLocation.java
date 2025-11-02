package simulator.regions;

import java.util.HashMap;
import java.util.List; // Add import
import java.util.Map;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.core.TreeNode;
import utils.CustomLogger;

public class BrokerWithRegionProcessingLocation extends BrokerWithRegion {

    private static final Logger logger = CustomLogger.getLogger(BrokerWithRegionProcessingLocation.class.getName());

    private final Map<Location, SimulationPublication> bestPublicationCache = new HashMap<>();
    private final Map<Location, Boolean> propagatedSubscriptions = new HashMap<>();
    
    // A cache to store the calculated key points for this broker's region.
    private List<Location> keyPointsCache = null;

    public BrokerWithRegionProcessingLocation(String name) {
        super(name);
    }

    public BrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }
    
    /**
     * A new private helper method that returns the cached key points.
     * If the cache is empty, it calculates them, stores them, and then returns them.
     */
    private List<Location> getOrCalculateKeyPoints() {
        if (this.keyPointsCache == null) {
            this.keyPointsCache = getRegion().getKeyPoints();
        }
        return this.keyPointsCache;
    }

    /**
     * This is the propagation logic for an intermediate broker.
     * It does not create a new proxy location, but forwards the one it receives.
     */
    @Override
    protected void propagateSubscription(SimulationSubscription s) {
        logger.fine(getName() + ": processing a subscription received from " + s.getSource().getName());
        addSubscription(s);

        if (s instanceof SubscriptionWithLocation sub) {
            // It uses the location from the INCOMING subscription for filtering.
            Location proxyLocation = sub.getLocation();
            logger.fine(String.format("%s: Received and processing proxy subscription for location %s.",
                    getName(), proxyLocation.toShortString()));

            if (propagatedSubscriptions.containsKey(proxyLocation)) {
                logger.fine(getName() + ": Proxy subscription for location " + proxyLocation.toShortString()
                        + " already propagated. Stopping upward propagation.");
                return;
            }

            propagatedSubscriptions.put(proxyLocation, true);
            
            if (getParentBroker() != null) {
                logger.fine(getName() + ": Forwarding proxy subscription upwards to parent " + getParentBroker().getName());
                // It forwards the original subscription object 's', preserving the proxy location.
                s.setSource(this);
                getParentBroker().processSubscription(s);
            }
        } else if (getParentBroker() != null) {
             // Fallback for other subscription types
            getParentBroker().processSubscription(s);
        }
    }


    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        logger.fine(getName() + ": processing publication from " + p.getSource().getName());

        if (p.getSource() != getParentBroker()) {
            propagatePublicationUpward(p);
        }

        if (p instanceof PublicationWithLocation pub) {
            processPublicationDownward(pub);
        }

        return null;
    }

    private void propagatePublicationUpward(SimulationPublication p) {
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null) {
            logger.fine(getName() + ": Propagating publication upwards to " + parentBroker.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parentBroker.processPublication(forwardedCopy);
        }
    }

    protected void processPublicationDownward(PublicationWithLocation pub) {
        if (getRegion() == null) {
            logger.severe(getName() + ": Cannot process downward propagation, region is not set.");
            return;
        }

        boolean isImprovement = false;
        // The key points are used for publication filtering.
        for (Location keyPoint : getOrCalculateKeyPoints()) {
            PublicationWithLocation cachedPub = (PublicationWithLocation) bestPublicationCache.get(keyPoint);

            String cachedLocationStr = (cachedPub == null) ? "none" : cachedPub.getLocation().toShortString();
            if (cachedPub == null ||
                    distanceSquared(pub.getLocation(), keyPoint) < distanceSquared(cachedPub.getLocation(), keyPoint)) {
                logger.fine(String.format("%s: New pub %s is an improvement over cached pub %s for key point %s.",
                        getName(), pub.getLocation().toShortString(), cachedLocationStr, keyPoint.toShortString()));
                bestPublicationCache.put(keyPoint, pub);
                isImprovement = true;
            }
        }
        
        if (isImprovement) {
            logger.fine(getName() + ": Publication is an improvement, forwarding to children.");
            for (TreeNode child : getChildren()) {
                if (child instanceof BrokerWithRegion childBroker && child != pub.getSource()) {
                    SimulationPublication forwardedCopy = pub.getPublication();
                    forwardedCopy.setSource(this);
                    childBroker.processPublication(forwardedCopy);
                }
            }
        } else {
            logger.fine(getName()
                    + ": Publication is not an improvement for any key point. Stopping downward propagation.");
        }
    }

    protected double distanceSquared(Location l1, Location l2) {
        double dx = l1.getX() - l2.getX();
        double dy = l1.getY() - l2.getY();
        double dz = l1.getZ() - l2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}