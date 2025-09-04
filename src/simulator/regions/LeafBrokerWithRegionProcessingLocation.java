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

    public LeafBrokerWithRegionProcessingLocation(String name) {
        super(name);
    }

    public LeafBrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
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
            if (lastPub == null ||
                distanceSquared(pub.getLocation(), subscriber.getLocation()) < distanceSquared(lastPub.getLocation(), subscriber.getLocation())) {
                logger.fine(getName() + ": Delivering publication to subscriber " + subscriber.getName() + " as it is an improvement.");
                subscriber.receive(pub);
            } else {
                logger.fine(getName() + ": Publication is not an improvement for " + subscriber.getName() + ". Filtering.");
            }
        }
    }
}