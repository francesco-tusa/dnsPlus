package simulator.regions;

import java.util.Map;
import java.util.logging.Logger;
import simulator.core.Location;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationPublication;
import simulator.events.SimulationSubscription;
import simulator.entities.SubscriberWithLocation;
import simulator.core.TreeNode;
import utils.CustomLogger;

public class LeafBrokerWithRegionProcessingRegion extends BrokerWithRegionProcessingRegion implements LeafBroker {

    private static final Logger logger = CustomLogger.getLogger(LeafBrokerWithRegionProcessingRegion.class.getName());

    public LeafBrokerWithRegionProcessingRegion(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    @Override
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        logger.fine(getName() + ": I am a leaf broker and I do not have children to forward subscriptions to.");
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
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
            if (nextNode instanceof SubscriberWithLocation) {
                if (entry.getValue() instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
                    if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                        logger.fine(getName() + ": Delivering publication to subscriber " + nextNode.getName());
                        SimulationPublication forwardedCopy = p.getPublication();
                        forwardedCopy.setSource(this);
                        ((SubscriberWithLocation) nextNode).receive(forwardedCopy);
                    }
                }
            }
        }
    }
}