package simulator.regions;

import java.util.Map;
import simulator.Location;
import simulator.PublicationWithLocation;
import simulator.SimulationPublication;
import simulator.SimulationSubscription;
import simulator.SubscriberWithLocation;
import simulator.TreeNode;

public class LeafBrokerWithRegionProcessingRegion extends BrokerWithRegionProcessingRegion implements LeafBroker {

    public LeafBrokerWithRegionProcessingRegion(String name) {
        super(name);
    }
    
    public LeafBrokerWithRegionProcessingRegion(String name, Location p1, Location p2) {
        super(name, p1, p2);
    }

    /**
     * Overrides the downward subscription propagation to stop it at the leaf.
     * This is the critical base case that prevents infinite recursion.
     */
    @Override
    protected void sendSubscriptionToChildren(SimulationSubscription newSubscription) {
        System.out.println(getName() + ": I am a leaf broker and I do not have children to forward subscriptions to.");
    }

    /**
     * Correctly handles publication for a leaf broker. It propagates publications
     * upward AND processes them for its own local subscribers.
     */
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
        for (Map.Entry<TreeNode, SimulationSubscription> entry : getSubscriptionsTable().entrySet()) {
            TreeNode nextNode = entry.getKey();
            if (nextNode instanceof SubscriberWithLocation) {
                if (entry.getValue() instanceof SubscriptionWithRegion subRegion && p instanceof PublicationWithLocation pubLocation) {
                    if (subRegion.getRegion().contains(pubLocation.getLocation())) {
                        System.out.println(getName() + ": Delivering publication to subscriber " + nextNode.getName());
                        SimulationPublication forwardedCopy = p.getPublication();
                        forwardedCopy.setSource(this);
                        ((SubscriberWithLocation) nextNode).receive(forwardedCopy);
                    }
                }
            }
        }
    }
}