package simulator.regions;

import java.util.logging.Level;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.regions.policy.PropagationRegionPolicy;
import simulator.regions.policy.StrictPropagationPolicy;
import utils.CustomLogger;

public class SpatialMatchLeafBroker extends SpatialMatchBroker implements LeafBroker {

    private static final Logger logger = CustomLogger.getLogger(SpatialMatchLeafBroker.class.getName());

    public SpatialMatchLeafBroker(String name, boolean forceSingleRegion, double threshold, PropagationRegionPolicy policy) {
        super(name, forceSingleRegion, threshold, policy);
    }
    
    public SpatialMatchLeafBroker(String name, Location p1, Location p2, boolean forceSingleRegion, double threshold, PropagationRegionPolicy policy) {
        super(name, p1, p2, forceSingleRegion, threshold, policy);
    }

    public SpatialMatchLeafBroker(String name) {
        super(name, true, 0.5, new StrictPropagationPolicy());
    }

    public SpatialMatchLeafBroker(String name, Location p1, Location p2) {
        super(name, p1, p2, true, 0.5, new StrictPropagationPolicy());
    }

    @Override
    public SimulationSubscription matchPublication(SimulationPublication p) {
        if (logger.isLoggable(Level.FINE)) {
            String sourceName = (p.getSource() != null) ? p.getSource().getName() : "NULL_SOURCE";
            logger.fine(getName() + ": processing a publication received from " + sourceName);
        }
        
        super.matchPublication(p);
        return null;
    }

    @Override
    public void processPublicationForLocalDelivery(SimulationPublication p) {
        // Handled by super.matchPublication
    }
}