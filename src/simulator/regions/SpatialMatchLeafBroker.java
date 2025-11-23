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

public class SpatialMatchLeafBroker extends SpatialMatchBroker implements LeafBroker {

    private static final Logger logger = CustomLogger.getLogger(SpatialMatchLeafBroker.class.getName());

    public SpatialMatchLeafBroker(String name) {
        super(name);
    }
    
    public SpatialMatchLeafBroker(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        logger.fine(getName() + ": processing a publication received from " + p.getSource().getName());

        // 1. Upward Propagation (Default Route)
        // This is allowed ONLY for Leaf Brokers receiving from their directly attached clients
        if (p.getSource() != getParentBroker()) {
            propagatePublicationUpward(p);
        }

        // 2. Local Delivery
        processPublicationForLocalDelivery(p);
        
        return null;
    }

    @Override
    public void processPublicationForLocalDelivery(SimulationPublication p) {
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode destinationNode = entry.getKey();

            if (destinationNode instanceof SubscriberWithLocation subscriber) {
                if (entry.getValue() instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
                    if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                        logger.fine(getName() + ": Delivering publication to local subscriber " + subscriber.getName());
                        // Final delivery
                        subscriber.receive(p);
                    }
                }
            }
        }
    }
}