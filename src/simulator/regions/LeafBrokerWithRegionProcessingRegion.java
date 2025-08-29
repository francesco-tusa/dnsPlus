package simulator.regions;

import java.util.Map;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

/**
 * A leaf broker that uses region-based routing. It has specialized logic
 * to forward publications from clients upwards, and to deliver publications
 * from its parent downwards to subscribers.
 */
public class LeafBrokerWithRegionProcessingRegion extends BrokerWithRegionProcessingRegion {

    public LeafBrokerWithRegionProcessingRegion(String name) {
        super(name);
    }
    
    /**
     * Constructor to create a leaf broker with a predefined region.
     * This is used by the BrokerFactory.
     */
    public LeafBrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }
    
    /**
     * A leaf broker's implementation of matchPublication. It correctly separates
     * the logic for handling upward vs. downward message flow.
     */
    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        System.out.println(getName() + ": processing a publication received from " + p.getSource().getName());

        // If the publication is from an attached client (a publisher), it's an UPWARD message.
        if (p.getSource() != getParentBroker()) {
            // Action: Propagate it up to our parent. Do NOT deliver locally on the way up.
            propagatePublicationUpward(p);
        } else {
            // This is a DOWNWARD message from our parent.
            // Action: Deliver to any local subscribers that match.
            processPublicationForLocalDelivery(p);
        }
        
        return null;
    }

    /**
     * Handles forwarding a publication UP to the parent broker.
     */
    private void propagatePublicationUpward(SimulationPublication p) {
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker != null) {
            System.out.println(getName() + ": forwarding publication to parent " + parentBroker.getName());
            SimulationPublication forwardedCopy = p.getPublication();
            forwardedCopy.setSource(this);
            parentBroker.processPublication(forwardedCopy);
        }
    }

    /**
     * For a leaf broker, this method delivers a publication to its own subscribers
     * if there is a match in its subscription table.
     */
    private void processPublicationForLocalDelivery(SimulationPublication p) {
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
            SimulationSubscription tableEntry = entry.getValue();

            // A leaf broker's subscription table should only point to subscribers.
            if (nextNode instanceof SubscriberWithLocation) {
                if (tableEntry instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
                    if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                        forwardPublicationToNode(p, nextNode);
                    }
                }
            }
        }
    }

    /**
     * Overrides the parent method. A leaf node does not have child brokers,
     * so this method is intentionally empty and prints a log message.
     */
    @Override
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        System.out.println(getName() + ": I am a leaf broker and I do not have children to forward subscriptions to.");
    }

    /**
     * Overrides the parent method. For a leaf broker, the next node for a publication
     * can only be a subscriber. This method delivers the publication.
     */
    @Override
    public void forwardPublicationToNode(SimulationPublication p, TreeNode next) {
        if (next instanceof SubscriberWithLocation subscriber) {
            System.out.println(getName() + ": Delivering publication to subscriber " + subscriber.getName());
            // Create a copy for delivery to prevent modifying the original publication object
            SimulationPublication deliveryCopy = p.getPublication();
            deliveryCopy.setSource(this); // The broker is the source for the subscriber
            subscriber.receive(deliveryCopy);
        } else {
             // This case should ideally not be reached with the new logic.
             System.err.println(getName() + ": Topology error. Leaf broker tried to forward publication to a non-subscriber node: " + next.getName());
        }
    }
}
