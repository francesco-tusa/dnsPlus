package simulator.regions;

import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;
import simulator.SubscriptionWithLocation;

/**
 * A leaf broker that uses location-based routing. It receives publications from
 * its parent and delivers them to its directly connected subscribers.
 */
public class LeafBrokerWithRegionProcessingLocation extends BrokerWithRegionProcessingLocation implements LeafBroker {

    public LeafBrokerWithRegionProcessingLocation(String name) {
        super(name);
    }

    public LeafBrokerWithRegionProcessingLocation(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing a publication received from " + p.getSource().getName());

        if (p.getSource() != getParentBroker()) {
            propagatePublicationUpward(p);
        }

        processPublicationForLocalDelivery(p);
        
        return null;
    }

    private void propagatePublicationUpward(SimulationPublication p) {
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null) {
            System.out.println(getName() + ": forwarding publication to parent " + parentBroker.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parentBroker.processPublication(forwardedCopy);
        }
    }
    
    @Override
    public void processPublicationForLocalDelivery(SimulationPublication p) {
        System.out.println(getName() + ": Processing publication for delivery to subscribers.");
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
                System.out.println(getName() + ": Delivering publication to subscriber " + subscriber.getName() + " as it is an improvement.");
                subscriber.receive(pub);
            } else {
                System.out.println(getName() + ": Publication is not an improvement for " + subscriber.getName() + ". Filtering.");
            }
        }
    }
}