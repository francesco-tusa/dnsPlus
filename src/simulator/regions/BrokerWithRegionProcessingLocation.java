package simulator.regions;

import java.util.HashMap;
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


    public BrokerWithRegionProcessingLocation(String name) {
        super(name);
    }

    public BrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public void processSubscription(SimulationSubscription s) {
        addSubscription(s);

        if (s instanceof SubscriptionWithLocation) {
            SubscriptionWithLocation subLoc = (SubscriptionWithLocation) s;
            if (propagatedSubscriptions.containsKey(subLoc.getLocation())) {
                logger.fine(getName() + ": Subscription for location " + subLoc.getLocation() + " already propagated. Stopping.");
                return;
            }
            propagatedSubscriptions.put(subLoc.getLocation(), true);

            if (getRegion() != null && getRegion().getBottomLeft() != null) {
                Location center = getRegion().getKeyPoints().get(8);
                
                SubscriptionWithLocation proxySubscription = new SubscriptionWithLocation(center);
                proxySubscription.setSource(s.getSource());
                s = proxySubscription;
            }
        }
        super.processSubscription(s);
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
            System.err.println(getName() + ": Cannot process downward propagation, region is not set.");
            return;
        }

        boolean isImprovement = false;
        for (Location keyPoint : getRegion().getKeyPoints()) {
            SimulationPublication cachedPub = bestPublicationCache.get(keyPoint);

            if (cachedPub == null ||
                distanceSquared(pub.getLocation(), keyPoint) < distanceSquared(((PublicationWithLocation) cachedPub).getLocation(), keyPoint))
            {
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
            logger.fine(getName() + ": Publication is not an improvement for any key point. Stopping downward propagation.");
        }
    }

    protected double distanceSquared(Location l1, Location l2) {
        double dx = l1.getX() - l2.getX();
        double dy = l1.getY() - l2.getY();
        double dz = l1.getZ() - l2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}