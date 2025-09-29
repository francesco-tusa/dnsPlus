package simulator.regions;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.entities.SubscriberWithLocation;
import simulator.core.TreeNode;
import simulator.events.SubscriptionWithLocation;
import utils.CustomLogger;

public class LeafBrokerWithRegionProcessingLocation extends BrokerWithRegionProcessingLocation implements LeafBroker {

    private static final Logger logger = CustomLogger.getLogger(LeafBrokerWithRegionProcessingLocation.class.getName());
    private Location proxyLocationCache = null;

    public LeafBrokerWithRegionProcessingLocation(String name) {
        super(name);
    }

    public LeafBrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    /**
     * Implements the propagation logic for the leaf broker. It has the unique
     * responsibility of creating the proxy location.
     */
    protected void propagateSubscription(SimulationSubscription s) {
        logger.fine(getName() + ": processing a subscription received from " + s.getSource().getName());
        addSubscription(s);

        if (s instanceof SubscriptionWithLocation sub) {
            logger.fine(String.format("%s: Received subscription for true location %s.",
                    getName(), sub.getLocation().toShortString()));
            
            // Check if the proxy location has been calculated yet.
            if (this.proxyLocationCache == null) {
                // If not, calculate it once and store it in the cache.
                this.proxyLocationCache = getRegion().getKeyPoints().get(8); // Index 8 is the center.
                logger.fine(getName() + ": First subscription received. Caching my proxy location: " + this.proxyLocationCache.toShortString());
            }

            logger.fine(String.format("%s: Creating and propagating proxy subscription with location %s.",
                    getName(), this.proxyLocationCache.toShortString()));

            SubscriptionWithLocation proxySubscription = new SubscriptionWithLocation(this.proxyLocationCache);
            proxySubscription.setSource(this);

            if (getParentBroker() != null) {
                getParentBroker().processSubscription(proxySubscription);
            }
        }
    }


    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        logger.fine(getName() + ": processing a publication received from " + p.getSource().getName());

        if (p.getSource() != getParentBroker()) {
            propagatePublicationUpward(p);
        }

        processPublicationForLocalDelivery(p);
        
        return null;
    }

    private void propagatePublicationUpward(SimulationPublication p) {
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null) {
            logger.fine(getName() + ": forwarding publication to parent " + parentBroker.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parentBroker.processPublication(forwardedCopy);
        }
    }
    
    @Override
    public void processPublicationForLocalDelivery(SimulationPublication p) {
        logger.fine(getName() + ": Processing publication for delivery to subscribers.");
        if (p instanceof PublicationWithLocation pub) {
            for (TreeNode child : getChildren()) {
                if (child instanceof SubscriberWithLocation subscriber) {
                    PublicationWithLocation finalCopy = (PublicationWithLocation) pub.getPublication();
                    finalCopy.setSource(this);
                    deliverToSubscriber(subscriber, finalCopy);
                }
            }
        }
    }

    private void deliverToSubscriber(SubscriberWithLocation subscriber, PublicationWithLocation pub) {
        SimulationSubscription sub = getSubscriptionsTable().get(subscriber);
        if (sub instanceof SubscriptionWithLocation) {
            PublicationWithLocation lastPub = subscriber.getLastReceivedPublication();
            
            String lastPubLocationStr = (lastPub == null) ? "none" : lastPub.getLocation().toShortString();
            double newDistance = distanceSquared(pub.getLocation(), subscriber.getLocation());
            double lastDistance = (lastPub == null) ? Double.POSITIVE_INFINITY : distanceSquared(lastPub.getLocation(), subscriber.getLocation());

            if (lastPub == null || newDistance < lastDistance) {
                logger.fine(String.format("%s: Delivering pub %s to %s. It is an improvement over last pub %s (NewDist^2: %.2f < OldDist^2: %.2f).",
                        getName(), pub.getLocation().toShortString(), subscriber.getName(), lastPubLocationStr, newDistance, lastDistance));
                subscriber.receive(pub);
            } else {
                logger.fine(String.format("%s: Filtering pub %s for %s. It is not an improvement over last pub %s (NewDist^2: %.2f >= OldDist^2: %.2f).",
                        getName(), pub.getLocation().toShortString(), subscriber.getName(), lastPubLocationStr, newDistance, lastDistance));
            }
        }
    }
}