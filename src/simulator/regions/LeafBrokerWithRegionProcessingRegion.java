package simulator.regions;

import java.util.Map;
import java.util.logging.Logger;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import utils.CustomLogger;

public class LeafBrokerWithRegionProcessingRegion extends BrokerWithRegionProcessingRegion implements LeafBroker {

    private static final Logger logger = CustomLogger.getLogger(LeafBrokerWithRegionProcessingRegion.class.getName());

    public LeafBrokerWithRegionProcessingRegion(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingRegion(String name, simulator.core.Location p1, simulator.core.Location p2) {
        super(name, p1, p2);
    }

    @Override
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        logger.fine(getName() + ": I am a leaf broker and I do not have children to forward subscriptions to.");
    }

    /**
     * The main controller for handling publications. It now separates the logic by
     * calling two dedicated methods: one for upward propagation and one for local delivery.
     */
    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        logger.fine(getName() + ": processing a publication received from " + p.getSource().getName());

        // --- FIX: Only propagate upward if the publication came from a local child
        // (e.g., a Publisher) ---
        // --- and NOT from the parent broker. ---
        if (p.getSource() != getParentBroker()) {
            // Call the method to handle conditional upward propagation.
            propagatePublicationUpward(p);
        }

        // Call the method to handle local delivery to subscribers.
        processPublicationForLocalDelivery(p);
        
        return null;
    }

    /**
     * This method now contains the specific logic to check the subscription table
     * for interest from the parent broker before propagating a publication upwards.
     */
    private void propagatePublicationUpward(SimulationPublication p) {
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker == null) return; // No parent to propagate to.

        // Check the subscription table specifically for an entry from the parent.
        SimulationSubscription parentSubscription = getSubscriptionsTable().get(parentBroker);

        if (parentSubscription instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
            // If the parent is interested, forward the publication.
            if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                logger.fine(getName() + ": Parent is interested. Forwarding publication upwards to " + parentBroker.getName());
                SimulationPublication forwardedCopy = p.getPublication();
                forwardedCopy.setSource(this);
                parentBroker.processPublication(forwardedCopy);
            }
        }
    }

    /**
     * This method contains the specific logic for checking the subscription table
     * and delivering the publication to any matching local subscribers.
     */
    @Override
    public void processPublicationForLocalDelivery(SimulationPublication p) {
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode destinationNode = entry.getKey();

            // Ensure we are only checking for local subscribers, not the parent.
            if (destinationNode instanceof SubscriberWithLocation subscriber) {
                if (entry.getValue() instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
                    if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                        logger.fine(getName() + ": Delivering publication to local subscriber " + subscriber.getName());
                        SimulationPublication finalCopy = p.getPublication();
                        finalCopy.setSource(this);
                        subscriber.receive(finalCopy);
                    }
                }
            }
        }
    }
}