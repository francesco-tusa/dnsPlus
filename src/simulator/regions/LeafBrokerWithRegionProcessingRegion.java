package simulator.regions;

import java.util.Map;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.core.TreeNode;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import utils.CustomLogger;

/**
 * A Leaf Broker implementation for Region-Based Routing.
 * * <p>Key Responsibilities:</p>
 * <ol>
 * <li>Behaves like a standard Region Broker for upward propagation.</li>
 * <li>Handles the final "Last Mile" delivery to connected Subscribers.</li>
 * </ol>
 */
public class LeafBrokerWithRegionProcessingRegion extends BrokerWithRegionProcessingRegion implements LeafBroker {

    private static final Logger logger = CustomLogger.getLogger(LeafBrokerWithRegionProcessingRegion.class.getName());

    public LeafBrokerWithRegionProcessingRegion(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }


    /**
     * Processes a publication.
     * 1. Propagates upward if necessary.
     * 2. Delivers locally to subscribers.
     */
    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        logger.fine(getName() + ": processing a publication received from " + p.getSource().getName());

        // 1. Upward Propagation
        // Only propagate upward if the publication came from a local child (Publisher)
        if (p.getSource() != getParentBroker()) {
            propagatePublicationUpward(p);
        }

        // 2. Local Delivery
        processPublicationForLocalDelivery(p);
        
        return null;
    }

    /**
     * Checks if the parent broker has an interest in this publication (based on the
     * Input View / Subscription Table) and forwards it if so.
     */
    private void propagatePublicationUpward(SimulationPublication p) {
        BrokerWithRegion parentBroker = getParentBroker();
        if (parentBroker == null) return;

        // Check the main subscription table for the parent's entry
        SimulationSubscription parentSubscription = getSubscriptionsTable().get(parentBroker);

        if (parentSubscription instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
            if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                logger.fine(getName() + ": Parent is interested. Forwarding publication upwards to " + parentBroker.getName());
                SimulationPublication forwardedCopy = p.getPublication();
                forwardedCopy.setSource(this);
                parentBroker.processPublication(forwardedCopy);
            }
        }
    }

    /**
     * Iterates through the subscription table to find matching LOCAL subscribers.
     */
    @Override
    public void processPublicationForLocalDelivery(SimulationPublication p) {
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode destinationNode = entry.getKey();

            // Only deliver to Subscribers (leaf nodes), not brokers
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