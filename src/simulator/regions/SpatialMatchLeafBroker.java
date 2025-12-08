package simulator.regions;

import java.util.logging.Logger;
import simulator.core.Location;
import simulator.entities.SubscriberWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import utils.CustomLogger;

public class SpatialMatchLeafBroker extends SpatialMatchBroker implements LeafBroker {

    private static final Logger logger = CustomLogger.getLogger(SpatialMatchLeafBroker.class.getName());

    public SpatialMatchLeafBroker(String name, boolean forceSingleRegion, double threshold) {
        super(name, forceSingleRegion, threshold);
    }
    
    public SpatialMatchLeafBroker(String name, Location p1, Location p2, boolean forceSingleRegion, double threshold) {
        super(name, p1, p2, forceSingleRegion, threshold);
    }

    // Legacy constructors for backward compatibility
    public SpatialMatchLeafBroker(String name) {
        super(name, true, 0.5);
    }

    public SpatialMatchLeafBroker(String name, Location p1, Location p2) {
        super(name, p1, p2, true, 0.5);
    }

    @Override
    protected String getRegionToLog(SubscriptionWithRegion sub) {
        // If the source is a direct subscriber (client), log the subscription region 
        // to see the bounding box the subscriber actually requested.
        if (sub.getSource() instanceof SubscriberWithLocation) {
            return sub.getRegion().toLogString();
        }
        
        // If the source is a peer broker, fall back to the non-leaf logic: 
        // log this broker's own region (MBR of its clients/children).
        return this.getRegion().toLogString();
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        String sourceName = (p.getSource() != null) ? p.getSource().getName() : "NULL_SOURCE";
        logger.fine(getName() + ": processing a publication received from " + sourceName);

        // DELEGATE TO PARENT:
        // The SpatialMatchBroker (parent) implementation already Checks 'inputStore' 
        // to find matching local subscribers (Local Delivery) or matching parent

        super.matchPublication(p);
        
        return null;
    }

    @Override
    public void processPublicationForLocalDelivery(SimulationPublication p) {
        // This method is required by the LeafBroker interface, but the logic 
        // is now handled entirely by super.matchPublication(p) via inputStore.forwardPublicationToNode().
        // We leave it empty to avoid duplicating delivery logic.
    }
}