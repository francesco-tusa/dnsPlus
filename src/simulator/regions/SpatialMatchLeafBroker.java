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

    public SpatialMatchLeafBroker(String name, boolean f, double t) { 
        super(name, f, t); 
    }
    
    public SpatialMatchLeafBroker(String name, Location p1, Location p2, boolean f, double t) { 
        super(name, p1, p2, f, t); 
    }

    // Legacy constructors for backward compatibility (defaults to Legacy Mode)
    public SpatialMatchLeafBroker(String name) {
        super(name, true, 0.0);
    }

    public SpatialMatchLeafBroker(String name, Location p1, Location p2) {
        super(name, p1, p2, true, 0.0);
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        logger.fine(getName() + ": processing a publication received from " + p.getSource().getName());

        // 1. ALWAYS attempt Local Delivery first (Short-Circuit)
        // This ensures local neighbors get the message instantly, regardless of where it came from.
        processPublicationForLocalDelivery(p);

        // 2. Upward Propagation
        // Only forward to the network (Parent) if the message originated locally (from a client).
        // If it came from the Parent, we stop here (Split Horizon).
        if (p.getSource() != getParentBroker()) {
            propagatePublicationUpward(p);
        }
        
        return null;
    }

    @Override
    public void processPublicationForLocalDelivery(SimulationPublication p) {
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode destinationNode = entry.getKey();

            // Only check Subscribers (Clients), ignore other Brokers (handled by propagateUp/Down)
            if (destinationNode instanceof SubscriberWithLocation subscriber) {
                if (entry.getValue() instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
                    
                    // Geometric Match: Does the Subscriber's Region contain the Publication?
                    if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                        logger.fine(getName() + ": Delivering publication to local subscriber " + subscriber.getName());
                        
                        // Final delivery to client
                        subscriber.receive(p);
                    }
                }
            }
        }
    }
}